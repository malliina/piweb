var webSocket;

var tableContent;

var onconnect = function (payload) {
    sendCmd("subscribe");
    setFeedback("Connected.");
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    if (event.event == "ping") {

    } else {
        prepend(event);
    }
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var prepend = function (e) {

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
var send = function (json) {
    var stringified = JSON.stringify(json);
    webSocket.send(stringified);
};
var sendCmd = function (word) {
    send({cmd: word});
};
var onChecked = function (number, isChecked) {
    var message = isChecked ? "on" : "off";
    send({msg: message, number: number});
};
$(document).ready(function () {
    tableContent = $("#logTableBody");
    $("#pwm").change(function () {
        send({msg: "pwm", value: this.value, number: this.id});
    });
});