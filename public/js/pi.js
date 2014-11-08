var webSocket;

var feedbackElem;

var onconnect = function (payload) {
    sendCmd("subscribe");
    setFeedback("Initializing...");
    sendMsg("status");
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    if (event.event == "ping") {

    } else {
        handle(event);
    }
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var handle = function (e) {
    switch (e.event) {
        case "status":
            onStatus(e);
            setFeedback("Connected.");
            break;
        case "digital":
            onDigitalStateChanged(e.pin.board, e.state);
            break;
        case "hw_pwm":
            onHardwarePwm(e.pin.board, e.value);
            break;
        case "blast_pwm":
            onBlastChanged(e.pin.board, e.value);
            break;
        case "color":
            var color = e.value;
            onColor(e.value);
            break;
        case "brightness":
            onBrightnessChanged(e.value);
            break;
        case "released":
            onReleased(e.pin.board);
            break;
        case "error":
            setErrorFeedback(e.message);
            break;
        case "feedback":
            setErrorFeedback(e.feedback);
            break;
        default:
            break;
    }
};
var onclose = function (payload) {
    setFeedback("Connection closed.");
};
var onerror = function (payload) {
    setFeedback("Connection error.");
};
var setFeedback = function (fb) {
    $('#status').html(fb);
};
var setErrorFeedback = function (fb) {
    feedbackElem.show();
    feedbackElem.html(fb);
};
var send = function (json) {
    $("feedback").empty();
    var stringified = JSON.stringify(json);
    webSocket.send(stringified);
};
var sendCmd = function (word) {
    send({cmd: word});
};
var sendMsg = function (msg) {
    send({msg: msg});
};
var onChecked = function (number, isChecked) {
    var message = isChecked ? "on" : "off";
    send({msg: message, number: number});
};
var onPwm = function (number, value) {
    sendPwm("pwm", number, value);
};
var onBlast = function (number, value) {
    sendPwm("blast", number, value);
};
var onBrightness = function (brightness) {
    send({msg: "brightness", value: parseInt(brightness)});
};
var onRgb = function (rgb) {
//    alert(rgb.r);
    send({msg: "rgb", red: rgb.r, green: rgb.g, blue: rgb.b});
};
var onRgb2 = function (rgb) {
    send({msg: "rgb", red: parseInt(rgb.r), green: parseInt(rgb.g), blue: parseInt(rgb.b)});
};
var sendPwm = function (msg, number, value) {
    send({msg: msg, number: parseInt(number), value: parseInt(value)});
};
var release = function (number) {
    send({msg: "release", number: parseInt(number)});
};
var releaseAll = function () {
    sendMsg("release_all");
};
var openDigital = function () {
    sendMsg("open");
};
var closeDigital = function () {
    sendMsg("close");
};

$(document).ready(function () {
    feedbackElem = $("#feedback");
    feedbackElem.hide();
});