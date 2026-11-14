import asyncio
import hashlib
import json
import os
import time
from collections import OrderedDict
from pathlib import Path


class TtlCache:
    def __init__(self, max_entries: int = 128) -> None:
        self.max_entries = max_entries
        self._entries: OrderedDict[str, tuple[float, object]] = OrderedDict()
        self._lock = asyncio.Lock()

    async def get(self, key: str, ttl_seconds: float) -> object | None:
        async with self._lock:
            entry = self._entries.get(key)
            if entry is None:
                return None
            created_at, value = entry
            if time.monotonic() - created_at >= ttl_seconds:
                del self._entries[key]
                return None
            self._entries.move_to_end(key)
            return value

    async def set(self, key: str, value: object) -> None:
        async with self._lock:
            self._entries.pop(key, None)
            self._entries[key] = (time.monotonic(), value)
            while len(self._entries) > self.max_entries:
                self._entries.popitem(last=False)


class DiskImageCache:
    def __init__(
        self,
        directory: str | Path | None = None,
        ttl_seconds: int = 86_400,
        max_bytes: int = 256 * 1024 * 1024,
    ) -> None:
        self.directory = Path(
            directory or os.getenv("MANGA_IMAGE_CACHE_DIR", "/tmp/fable-manga-image-cache")
        )
        self.ttl_seconds = ttl_seconds
        self.max_bytes = max_bytes
        self._lock = asyncio.Lock()

    def _paths(self, key: str) -> tuple[Path, Path]:
        digest = hashlib.sha256(key.encode("utf-8")).hexdigest()
        return self.directory / f"{digest}.body", self.directory / f"{digest}.json"

    async def get(self, key: str) -> tuple[bytes, str] | None:
        async with self._lock:
            body_path, metadata_path = self._paths(key)
            try:
                if time.time() - body_path.stat().st_mtime >= self.ttl_seconds:
                    body_path.unlink(missing_ok=True)
                    metadata_path.unlink(missing_ok=True)
                    return None
                body = body_path.read_bytes()
                media_type = json.loads(metadata_path.read_text("utf-8"))["media_type"]
                return body, media_type
            except (OSError, ValueError, KeyError, TypeError):
                body_path.unlink(missing_ok=True)
                metadata_path.unlink(missing_ok=True)
                return None

    async def set(self, key: str, body: bytes, media_type: str) -> None:
        if len(body) > self.max_bytes:
            return
        async with self._lock:
            self.directory.mkdir(parents=True, exist_ok=True)
            body_path, metadata_path = self._paths(key)
            body_temp = body_path.with_suffix(".body.tmp")
            metadata_temp = metadata_path.with_suffix(".json.tmp")
            body_temp.write_bytes(body)
            metadata_temp.write_text(json.dumps({"media_type": media_type}), "utf-8")
            body_temp.replace(body_path)
            metadata_temp.replace(metadata_path)
            self._evict()

    def _evict(self) -> None:
        now = time.time()
        entries: list[tuple[float, int, Path]] = []
        total_bytes = 0
        for body_path in self.directory.glob("*.body"):
            try:
                stat = body_path.stat()
            except OSError:
                continue
            if now - stat.st_mtime >= self.ttl_seconds:
                body_path.unlink(missing_ok=True)
                body_path.with_suffix(".json").unlink(missing_ok=True)
                continue
            total_bytes += stat.st_size
            entries.append((stat.st_mtime, stat.st_size, body_path))

        for _, size, body_path in sorted(entries):
            if total_bytes <= self.max_bytes:
                break
            body_path.unlink(missing_ok=True)
            body_path.with_suffix(".json").unlink(missing_ok=True)
            total_bytes -= size
