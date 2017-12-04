hterm.defaultStorage = new lib.Storage.Memory();

var t = new hterm.Terminal("cloudterm");

t.getPrefs().set("send-encoding", "utf-8");
t.getPrefs().set("receive-encoding", "utf-8");

// t.getPrefs().set("use-default-window-copy", true);
t.getPrefs().set("clear-selection-after-copy", true);
t.getPrefs().set("copy-on-select", true);
t.getPrefs().set("ctrl-c-copy", true);
t.getPrefs().set("ctrl-v-paste", true);
// t.getPrefs().set("cursor-color", "black");
// t.getPrefs().set("background-color", "white");
// t.getPrefs().set("font-size", 12);
// t.getPrefs().set("foreground-color", "black");
// t.getPrefs().set("cursor-blink", false);
// t.getPrefs().set("scrollbar-visible", true);
// t.getPrefs().set("scroll-wheel-move-multiplier", 0.1);
// t.getPrefs().set("user-css", "/afx/resource/?p=css/hterm.css");
t.getPrefs().set("enable-clipboard-notice", true);

t.onTerminalReady = function () {

    app.onTerminalInit();

    var io = t.io.push();

    io.onVTKeystroke = function (str) {
        app.onCommand(str);
    };

    io.sendString = io.onVTKeystroke;

    io.onTerminalResize = function (columns, rows) {
        app.resizeTerminal(columns, rows);
    };

    t.installKeyboard();
    app.onTerminalReady();

};

let wsut = document.getElementById('wsut').value;
let ws = new WebSocket("ws://" + location.host + "/terminal/ws?wsut=" + wsut);

ws.onopen = () => {
    t.decorate(document.querySelector('#terminal'));
    t.showOverlay("Connection established", 1000);
}

ws.onerror = () => {
    t.showOverlay("Connection error", 3000);
}

ws.onclose = () => {
    t.showOverlay("Connection closed", 3000);
}

ws.onmessage = (e) => {
    let data = JSON.parse(e.data);
    switch (data.type) {
        case "TERMINAL_PRINT":
            if (data.refresh === 'true') {
                t.clearHome();
            }
            t.io.print(data.text);
    }
}

function action(type, data) {
    let action = Object.assign({
        type
    }, data);

    return JSON.stringify(action);
}

let app = {
    onTerminalInit() {
        ws.send(action("TERMINAL_INIT"));
    },
    onCommand(command) {
        ws.send(action("TERMINAL_COMMAND", {
            command
        }));
    },
    resizeTerminal(columns, rows) {
        ws.send(action("TERMINAL_RESIZE", {
            columns, rows
        }));
    },
    onTerminalReady() {
        // ws.send(action("TERMINAL_READY"));
    }
};