"""Prepare a separate Helios source copy with prefixed database object names."""
import re
import shutil
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[2]
output = Path(sys.argv[1]).resolve()
if output == root or root in output.parents:
    raise SystemExit('Output must be outside the source directory')
if output.exists():
    raise SystemExit('Output directory must not exist')
# Copy sources, never build artifacts or local credentials.
shutil.copytree(root, output, ignore=shutil.ignore_patterns('.git', 'target', 'dist', '.env', '__pycache__'))
schema = (root / 'db/001_schema.sql').read_text()
names = re.findall(r'CREATE\s+(?:TABLE|FUNCTION|INDEX|TRIGGER)\s+(\w+)', schema, re.I)
names = {name: 'ml53434963_' + name for name in names}
pattern = re.compile(r'\b(' + '|'.join(map(re.escape, names)) + r')\b')
def replace(text):
    return pattern.sub(lambda match: names[match.group()], text)

for folder in ('db', 'tests'):
    for path in (output / folder).glob('*.sql'):
        path.write_text(replace(path.read_text()))
for path in (output / 'src/main/java').rglob('*.java'):
    text = path.read_text()
    text = re.sub(r'(@Table\(name = ")([^"]+)(")', lambda m: m[1] + replace(m[2]) + m[3], text)
    text = re.sub(r'(createNativeQuery\(")([^"]+)(")', lambda m: m[1] + replace(m[2]) + m[3], text)
    path.write_text(text)
path = output / 'tests/create_user.py'
path.write_text(replace(path.read_text()))
print(output)
