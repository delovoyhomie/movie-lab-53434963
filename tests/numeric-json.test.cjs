const fs = require("node:fs"),
    vm = require("node:vm"),
    assert = require("node:assert/strict");
vm.runInThisContext(fs.readFileSync(__dirname + "/../src/main/webapp/numeric-json.js", "utf8"));
const data = parseExactJson(
    '{"id":9223372036854775807,"name":"a \\\"id\\\":9223372036854775807", "budget":1.5, "length":90}',
);
assert.equal(data.id, "9223372036854775807");
assert.equal(data.length, 90);
assert.equal(data.budget, 1.5);
assert.equal(
    encodeJson({ oscarsCount: "9223372036854775807", name: "123" }),
    '{"oscarsCount":9223372036854775807,"name":"123"}',
);
assert.equal(positiveLong("00012", "x"), "12");
assert.throws(() => positiveLong("9223372036854775808", "x"));
assert.throws(() => positiveLong("0", "x"));
assert.equal(
    parseExactJson(JSON.stringify({ name: '"oscarsCount":"123"' })).name,
    '"oscarsCount":"123"',
);
console.log("PASS numeric JSON");
