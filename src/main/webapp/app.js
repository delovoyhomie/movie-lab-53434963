"use strict";

const getElement = (id) => document.getElementById(id);
const state = {
    csrf: null,
    page: 0,
    size: 10,
    view: "movies",
    options: null,
    revision: null,
    busy: false,
    editing: null,
    detail: null,
    special: null,
    connectionError: false,
};
const columns = [
    ["id", "ID"],
    ["name", "Название"],
    ["coordinates", "Координаты"],
    ["creationDate", "Дата создания"],
    ["oscarsCount", "Оскары"],
    ["budget", "Бюджет"],
    ["totalBoxOffice", "Сборы"],
    ["mpaaRating", "MPAA"],
    ["director", "Режиссёр"],
    ["screenwriter", "Сценарист"],
    ["operator", "Оператор"],
    ["length", "Длительность"],
    ["goldenPalmCount", "Золотые пальмы"],
    ["tagline", "Слоган"],
    ["genre", "Жанр"],
];
const stringColumns = [
    ["name", "Название"],
    ["tagline", "Слоган"],
    ["genre", "Жанр"],
    ["mpaaRating", "MPAA"],
    ["director", "Режиссёр"],
    ["screenwriter", "Сценарист"],
    ["operator", "Оператор"],
];
const labels = Object.fromEntries([
    ...columns,
    ["version", "Версия"],
    ["eyeColor", "Цвет глаз"],
    ["hairColor", "Цвет волос"],
    ["location", "Местоположение"],
    ["height", "Рост"],
    ["nationality", "Гражданство"],
]);
function createElement(tag, text) {
    const element = document.createElement(tag);
    if (text !== undefined) {
        element.textContent = String(text);
    }
    return element;
}

function createOption(value, label) {
    const element = createElement("option", label);
    element.value = value;
    return element;
}

function fillOptions(select, items, blank = false) {
    select.replaceChildren();
    if (blank) {
        select.append(createOption("", "— не указано —"));
    }
    for (const [value, label] of items) {
        select.append(createOption(value, label));
    }
}

async function api(path, method = "GET", body) {
    const headers = {
        "Content-Type": "application/json",
    };
    if (state.csrf) {
        headers["X-CSRF-Token"] = state.csrf;
    }
    const response = await fetch("api/" + path, {
        method,
        headers,
        body: body === undefined ? undefined : encodeJson(body),
        credentials: "same-origin",
    });
    const data =
        response.status === 204
            ? null
            : await response
                  .text()
                  .then(parseExactJson)
                  .catch(() => ({
                      error: "Сервер вернул некорректный ответ",
                  }));
    if (!response.ok) {
        if (response.status === 401 && path !== "auth/login") {
            showLogin();
        }
        throw new Error(data?.error || "Ошибка " + response.status);
    }
    return data;
}

function showStatus(text) {
    getElement("status").textContent = text;
}

function showLogin() {
    state.csrf = null;
    getElement("loginPanel").hidden = false;
    getElement("app").hidden = true;
    for (const detail of document.querySelectorAll("dialog")) {
        detail.close();
    }
}

async function showApplication(user) {
    state.csrf = user.csrf;
    getElement("who").textContent = user.login;
    getElement("app").hidden = false;
    getElement("loginPanel").hidden = true;
    state.options = await api("references");
    state.revision = (await api("movies/revision")).revision;
    await refresh();
    showStatus("Автообновление включено · проверка каждые 2 секунды");
}

getElement("loginForm").onsubmit = async (event) => {
    event.preventDefault();
    try {
        const data = Object.fromEntries(new FormData(event.target));
        await showApplication(await api("auth/login", "POST", data));
        getElement("loginError").textContent = "";
        event.target.password.value = "";
    } catch (error) {
        getElement("loginError").textContent = error.message;
    }
};
getElement("logout").onclick = async () => {
    try {
        await api("auth/logout", "POST", {});
        showLogin();
    } catch (error) {
        showStatus(error.message);
    }
};
fillOptions(getElement("filterColumn"), stringColumns);
fillOptions(getElement("sortColumn"), [["id", "ID"], ...stringColumns]);
fillOptions(
    getElement("sourceGenre"),
    ["ACTION", "WESTERN", "COMEDY", "FANTASY"].map((x) => [x, x]),
);
fillOptions(
    getElement("targetGenre"),
    ["ACTION", "WESTERN", "COMEDY", "FANTASY"].map((x) => [x, x]),
);
getElement("targetGenre").value = "COMEDY";
for (const button of document.querySelectorAll("[data-view]")) {
    button.onclick = async () => {
        state.view = button.dataset.view;
        for (const v of ["movies", "references", "special"]) {
            getElement(v + "View").hidden = v !== state.view;
        }
        for (const navigationButton of document.querySelectorAll("[data-view]")) {
            navigationButton.classList.toggle("active", navigationButton === button);
        }
        try {
            await refresh();
        } catch (error) {
            showStatus(error.message);
        }
    };
}
for (const button of document.querySelectorAll("[data-close]")) {
    button.onclick = () => getElement(button.dataset.close).close();
}

function formatValue(value) {
    if (value === null || value === undefined) {
        return "—";
    }
    if (typeof value === "object") {
        if ("eyeColor" in value) {
            return "#" + value.id + " " + value.name;
        }
        if ("z" in value) {
            return (
                "#" +
                value.id +
                " " +
                value.name +
                " (" +
                value.x +
                ", " +
                value.y +
                ", " +
                value.z +
                ")"
            );
        }
        return "#" + value.id + " (" + value.x + ", " + value.y + ")";
    }
    return String(value);
}

function renderTable(container, items, tableColumns, kind) {
    container.replaceChildren();
    if (!items.length) {
        container.append(createElement("p", "Ничего не найдено."));
        return;
    }
    const tableElement = createElement("table"),
        headerRow = createElement("tr");
    for (const [, label] of tableColumns) {
        headerRow.append(createElement("th", label));
    }
    headerRow.append(createElement("th", "Действия"));
    const thead = createElement("thead");
    thead.append(headerRow);
    tableElement.append(thead);
    const tbody = createElement("tbody");
    for (const item of items) {
        const row = createElement("tr");
        for (const [key] of tableColumns) {
            row.append(createElement("td", formatValue(item[key])));
        }
        const actions = createElement("td");
        for (const [label, action] of [
            ["Открыть", () => openDetail(kind, item.id)],
            ["Изменить", () => openEditor(kind, item.id)],
        ]) {
            const button = createElement("button", label);
            button.onclick = () => action().catch((error) => showStatus(error.message));
            actions.append(button);
        }
        row.append(actions);
        tbody.append(row);
    }
    tableElement.append(tbody);
    container.append(tableElement);
}

async function loadMovies() {
    const params = new URLSearchParams({
        page: state.page,
        size: state.size,
        column: getElement("filterColumn").value,
        q: getElement("filterText").value,
        sort: getElement("sortColumn").value,
        desc: getElement("sortDesc").value,
    });
    const data = await api("movies?" + params);
    state.page = data.page;
    renderTable(getElement("movieTable"), data.items, columns, "movies");
    getElement("pageInfo").textContent =
        `Страница ${data.page + 1} из ${Math.max(1, Math.ceil(data.total / state.size))} · всего ${data.total}`;
    getElement("prev").disabled = state.page === 0;
    getElement("next").disabled = (state.page + 1) * state.size >= data.total;
}
const refColumns = {
    coordinates: [
        ["id", "ID"],
        ["x", "X"],
        ["y", "Y"],
    ],
    locations: [
        ["id", "ID"],
        ["x", "X"],
        ["y", "Y"],
        ["z", "Z"],
        ["name", "Название"],
    ],
    persons: [
        ["id", "ID"],
        ["name", "Имя"],
        ["eyeColor", "Глаза"],
        ["hairColor", "Волосы"],
        ["location", "Местоположение"],
        ["height", "Рост"],
        ["nationality", "Гражданство"],
    ],
};

async function loadReferences() {
    const kind = getElement("referenceKind").value;
    renderTable(
        getElement("referenceTable"),
        await api("references/" + kind),
        refColumns[kind],
        kind,
    );
}

async function refresh() {
    if (state.view === "movies") {
        await loadMovies();
    }
    if (state.view === "references") {
        await loadReferences();
    }
    if (state.view === "special" && state.special) {
        await runSpecialOperation(state.special);
    }
    if (getElement("detail").open && state.detail) {
        await updateDetail();
    }
}

getElement("filterForm").onsubmit = (event) => {
    event.preventDefault();
    state.page = 0;
    loadMovies().catch((error) => showStatus(error.message));
};
getElement("resetFilter").onclick = () => {
    getElement("filterText").value = "";
    getElement("filterColumn").value = "name";
    getElement("sortColumn").value = "id";
    getElement("sortDesc").value = "false";
    state.page = 0;
    loadMovies().catch((error) => showStatus(error.message));
};
getElement("prev").onclick = () => {
    state.page--;
    loadMovies().catch((error) => showStatus(error.message));
};
getElement("next").onclick = () => {
    state.page++;
    loadMovies().catch((error) => showStatus(error.message));
};
getElement("pageSize").onchange = () => {
    state.size = +getElement("pageSize").value;
    state.page = 0;
    loadMovies().catch((error) => showStatus(error.message));
};
getElement("referenceKind").onchange = () =>
    loadReferences().catch((error) => showStatus(error.message));
getElement("findForm").onsubmit = (event) => {
    event.preventDefault();
    openDetail("movies", event.target.elements.namedItem("id").value).catch((error) =>
        showStatus(error.message),
    );
};
function getEndpoint(kind, id) {
    return (
        (kind === "movies" ? "movies" : "references/" + kind) + (id === undefined ? "" : "/" + id)
    );
}

function renderDetails(object) {
    const dl = createElement("dl");
    for (const [key, value] of Object.entries(object)) {
        dl.append(createElement("dt", labels[key] || key));
        const dd = createElement("dd");
        if (value && typeof value === "object") {
            dd.className = "nested";
            dd.append(renderDetails(value));
        } else {
            dd.textContent = formatValue(value);
        }
        dl.append(dd);
    }
    return dl;
}

async function openDetail(kind, id) {
    state.detail = {
        kind,
        id,
    };
    await updateDetail();
    getElement("detail").showModal();
}

async function updateDetail() {
    try {
        const { kind, id } = state.detail;
        const data = await api(getEndpoint(kind, id));
        state.detail.data = data;
        getElement("detailTitle").textContent =
            (kind === "movies" ? "Фильм" : "Запись справочника") + " #" + id;
        getElement("detailBody").replaceChildren(renderDetails(data));
    } catch (error) {
        getElement("detail").close();
        state.detail = null;
        throw error;
    }
}

getElement("editDetail").onclick = async () => {
    const { kind, id } = state.detail;
    getElement("detail").close();
    try {
        await openEditor(kind, id);
    } catch (error) {
        showStatus(error.message);
    }
};
let confirmAction;
function confirmDelete(text, action) {
    getElement("confirmText").textContent = text;
    getElement("confirmError").textContent = "";
    confirmAction = action;
    getElement("confirmation").showModal();
}

getElement("confirmYes").onclick = async () => {
    const button = getElement("confirmYes");
    button.disabled = true;
    try {
        await confirmAction();
        getElement("confirmation").close();
        await refreshAfterChange();
    } catch (error) {
        getElement("confirmError").textContent = error.message;
    } finally {
        button.disabled = false;
    }
};
getElement("deleteDetail").onclick = () => {
    const detail = state.detail;
    confirmDelete(
        detail.kind === "movies"
            ? `Удалить фильм #${detail.id}? Справочные записи сохранятся, так как могут использоваться другими фильмами.`
            : `Удалить запись #${detail.id}? Все зависимые фильмы тоже будут удалены. При удалении местоположения удалятся также связанные люди.`,
        async () => {
            await api(
                getEndpoint(detail.kind, detail.id) + "?version=" + detail.data.version,
                "DELETE",
            );
            getElement("detail").close();
            state.detail = null;
        },
    );
};
const numberField = (name, label, required = false, step = "any", min = null, max = null) => ({
    name,
    label,
    type: "number",
    required,
    step,
    min,
    max,
});
const textField = (name, label, required = false) => ({
    name,
    label,
    type: "text",
    required,
});
const selectField = (name, label, items, required = false) => ({
    name,
    label,
    type: "select",
    items,
    required,
});
function getFields(kind) {
    const options = state.options,
        referenceOptions = (k) => options[k].map((x) => [x.id, formatValue(x)]),
        enumOptions = (k) => options[k].map((x) => [x, x]);
    if (kind === "movies") {
        return [
            textField("name", "Название", true),
            selectField("coordinatesId", "Координаты", referenceOptions("coordinates"), true),
            numberField("oscarsCount", "Оскары (пусто = нет)", "", 1, 1),
            numberField("budget", "Бюджет", true, "any", Number.MIN_VALUE),
            numberField("totalBoxOffice", "Кассовые сборы", true, "any", Number.MIN_VALUE),
            selectField("mpaaRating", "Рейтинг MPAA", enumOptions("ratings"), true),
            selectField("directorId", "Режиссёр", referenceOptions("persons"), true),
            selectField("screenwriterId", "Сценарист", referenceOptions("persons")),
            selectField("operatorId", "Оператор", referenceOptions("persons")),
            numberField("length", "Длительность, мин", true, 1, 1, 2147483647),
            numberField("goldenPalmCount", "Золотые пальмы", false, 1, 1),
            textField("tagline", "Слоган (пусто = NULL)"),
            selectField("genre", "Жанр", enumOptions("genres")),
        ];
    }
    if (kind === "coordinates") {
        return [
            numberField("x", "X", true, 1, -2147483648, 2147483647),
            numberField("y", "Y (не больше 2)", true, "any", null, 2),
        ];
    }
    if (kind === "locations") {
        return [
            numberField("x", "X", true, 1, -2147483648, 2147483647),
            numberField("y", "Y", true, 1, -2147483648, 2147483647),
            numberField("z", "Z", true),
            textField("name", "Название (пустая строка допустима)"),
        ];
    }
    return [
        textField("name", "Имя", true),
        selectField("eyeColor", "Цвет глаз", enumOptions("colors"), true),
        selectField("hairColor", "Цвет волос", enumOptions("colors"), true),
        selectField("locationId", "Местоположение", referenceOptions("locations")),
        numberField("height", "Рост", true, "any", Number.MIN_VALUE),
        selectField("nationality", "Гражданство", enumOptions("countries")),
    ];
}

async function openEditor(kind, id) {
    state.options = await api("references");
    const data = id === undefined ? {} : await api(getEndpoint(kind, id));
    state.editing = {
        kind,
        id,
        version: data.version,
        fields: getFields(kind),
    };
    getElement("editorTitle").textContent =
        (id === undefined ? "Создать: " : "Изменить: ") +
        {
            movies: "фильм",
            persons: "человек",
            coordinates: "координаты",
            locations: "местоположение",
        }[kind];
    getElement("editorError").textContent = "";
    getElement("editorWarning").textContent = "";
    getElement("editorFields").replaceChildren();
    for (const field of state.editing.fields) {
        const label = createElement("label", field.label + (field.required ? " *" : ""));
        const input = createElement(field.type === "select" ? "select" : "input");
        input.name = field.name;
        input.required = !!field.required;
        if (field.type === "select") {
            fillOptions(input, field.items, true);
        } else {
            input.type = ["oscarsCount", "goldenPalmCount"].includes(field.name)
                ? "text"
                : field.type;
            if (input.type === "text" && field.type === "number") {
                input.inputMode = "numeric";
            }
            if (input.type === "number") {
                input.step = field.step;
                if (field.min !== null) {
                    input.min = field.min;
                }
                if (field.max !== null) {
                    input.max = field.max;
                }
            }
        }
        let value = data[field.name];
        if (field.name.endsWith("Id")) {
            value = data[field.name.slice(0, -2)]?.id;
        }
        input.value = value ?? "";
        label.append(input);
        getElement("editorFields").append(label);
    }
    getElement("editor").showModal();
}

getElement("addMovie").onclick = () =>
    openEditor("movies").catch((error) => showStatus(error.message));
getElement("addReference").onclick = () =>
    openEditor(getElement("referenceKind").value).catch((error) => showStatus(error.message));
function readEditorForm(form, kind, version, fields) {
    const formData = new FormData(form);
    const body = { version };
    for (const field of fields) {
        const value = formData.get(field.name);
        if (value === "") {
            body[field.name] = kind === "locations" && field.name === "name" ? "" : null;
        } else if (
            ["oscarsCount", "goldenPalmCount"].includes(field.name) ||
            field.name.endsWith("Id")
        ) {
            body[field.name] = positiveLong(value, field.label);
        } else if (field.type === "number") {
            const number = Number(value);
            if (!Number.isFinite(number)) {
                throw new Error(field.label + ": некорректное число");
            }
            if ((field.step === 1 || field.name.endsWith("Id")) && !Number.isSafeInteger(number)) {
                throw new Error(
                    field.label + ": число вне точного целочисленного диапазона браузера",
                );
            }
            body[field.name] = number;
        } else {
            body[field.name] = value;
        }
    }
    return body;
}

getElement("editorForm").onsubmit = async (event) => {
    event.preventDefault();
    const button = getElement("saveButton");
    button.disabled = true;
    try {
        const { kind, id, version, fields } = state.editing;
        const body = readEditorForm(event.target, kind, version, fields);
        await api(getEndpoint(kind, id), id === undefined ? "POST" : "PUT", body);
        getElement("editor").close();
        await refreshAfterChange();
    } catch (error) {
        getElement("editorError").textContent = error.message;
    } finally {
        button.disabled = false;
    }
};

async function refreshAfterChange() {
    state.options = await api("references");
    await refresh();
    showStatus("Изменения сохранены. Другие клиенты увидят их автоматически.");
}

getElement("operation").onchange = () => {
    const operation = getElement("operation").value;
    getElement("textField").hidden = !["tagline", "delete-tagline"].includes(operation);
    getElement("genreField").hidden = !["genre-less", "redistribute"].includes(operation);
    getElement("targetField").hidden = operation !== "redistribute";
    state.special = null;
    getElement("specialResult").replaceChildren();
};

async function runSpecialOperation(args) {
    const { op: operation, text, source, target } = args;
    let data;
    if (operation === "tagline") {
        data = await api("special/tagline?text=" + encodeURIComponent(text));
    }
    if (operation === "genre-less") {
        data = await api("special/genre-less?genre=" + source);
    }
    if (operation === "writers") {
        data = await api("special/writers");
    }
    if (operation === "delete-tagline") {
        data = await api("special/delete-tagline", "POST", {
            text,
        });
    }
    if (operation === "redistribute") {
        data = await api("special/redistribute", "POST", {
            source,
            target,
        });
    }
    if (Array.isArray(data)) {
        renderTable(
            getElement("specialResult"),
            data,
            operation === "writers" ? refColumns.persons : columns,
            operation === "writers" ? "persons" : "movies",
        );
    } else {
        getElement("specialResult").replaceChildren(
            createElement(
                "p",
                operation === "redistribute"
                    ? "Перенесено наград: " + data.transferred
                    : data.deleted
                      ? "Удалён фильм #" + data.id
                      : "Фильм с таким слоганом не найден.",
            ),
        );
    }
    return data;
}

getElement("specialForm").onsubmit = async (event) => {
    event.preventDefault();
    const args = {
        op: getElement("operation").value,
        text: getElement("specialText").value,
        source: getElement("sourceGenre").value,
        target: getElement("targetGenre").value,
    };
    state.special = null;
    try {
        if (args.op === "delete-tagline") {
            confirmDelete("Удалить один фильм с точным слоганом «" + args.text + "»?", () =>
                runSpecialOperation(args),
            );
            return;
        }
        if (args.op === "redistribute") {
            await runSpecialOperation(args);
            await refreshAfterChange();
        } else {
            await runSpecialOperation(args);
            state.special = args;
        }
    } catch (error) {
        getElement("specialResult").replaceChildren(createElement("p", error.message));
    }
};
setInterval(async () => {
    if (!state.csrf || state.busy) {
        return;
    }
    state.busy = true;
    try {
        const revision = (await api("movies/revision")).revision;
        if (revision !== state.revision) {
            state.revision = revision;
            state.options = await api("references");
            if (getElement("editor").open) {
                getElement("editorWarning").textContent =
                    "Данные в системе изменились. Ваш ввод сохранён. При конфликте сервер попросит заново открыть форму.";
            }
            await refresh();
        }
        if (state.connectionError) {
            showStatus("Связь с сервером восстановлена · автообновление включено");
            state.connectionError = false;
        }
    } catch (error) {
        state.connectionError = true;
        showStatus("Автообновление: " + error.message);
    } finally {
        state.busy = false;
    }
}, 2000);
api("auth/me")
    .then(showApplication)
    .catch(() => showLogin());
