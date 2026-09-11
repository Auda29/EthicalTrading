"""Generate an empty 14x5x6 GameTest fixture without third-party packages."""
import gzip, struct
from pathlib import Path

def text(s):
    b = s.encode(); return struct.pack('>H', len(b)) + b

def tag(kind, name, data): return bytes([kind]) + text(name) + data
size = tag(9, 'size', b'\x03' + struct.pack('>i3i', 3, 14, 5, 6))
air = tag(8, 'Name', text('minecraft:air')) + b'\x00'
palette = tag(9, 'palette', b'\x0a' + struct.pack('>i', 1) + air)
empty = lambda name: tag(9, name, b'\x0a' + struct.pack('>i', 0))
nbt = b'\x0a\x00\x00' + size + palette + empty('blocks') + empty('entities') + b'\x00'
p = Path(__file__).resolve().parents[1] / 'src/gametest/resources/data/ethical-trading-test/structure/hall.nbt'
p.parent.mkdir(parents=True, exist_ok=True)
p.write_bytes(gzip.compress(nbt, mtime=0))
print(p)
