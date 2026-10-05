"use strict";
// JSON числа Long могут быть больше Number.MAX_SAFE_INTEGER.
// Большие целые читаем как строки, не меняя содержимое строковых полей.
function parseExactJson(source) {
    return JSON.parse(
        source.replace(/"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g, (token) => {
            if (/^-?\d+$/.test(token) && !Number.isSafeInteger(Number(token)))
                return '"' + token + '"';
            return token;
        }),
    );
}
// Только явно перечисленные целочисленные поля преобразуются обратно в JSON number.
function encodeJson(body) {
    return JSON.stringify(body).replace(
        /("(?:oscarsCount|goldenPalmCount|coordinatesId|directorId|screenwriterId|operatorId|locationId|version)"\s*:\s*)"(-?\d+)"/g,
        "$1$2",
    );
}
function positiveLong(value, label) {
    if (!/^\d+$/.test(value) || BigInt(value) < 1n || BigInt(value) > 9223372036854775807n)
        throw new Error(label + ": необходимо целое число от 1 до 9223372036854775807");
    return BigInt(value).toString();
}
