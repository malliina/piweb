var openBtn;
var closeBtn;
var digitalDiv;

var onOpened = function () {
    openBtn.hide();
    closeBtn.show();
    digitalDiv.show();
};
var onClosed = function () {
    closeBtn.hide();
    openBtn.show();
    digitalDiv.hide();
};
var onHardwarePwm = function (boardNum, value) {
    $("#" + boardNum).val(value);
};
var onDigitalStateChanged = function (boardNum, state) {

};
var onBlast = function (boardNum, value) {

};
var onReleased = function (boardNum) {

};
$(document).ready(function () {
    openBtn = $("#open-btn");
    closeBtn = $("#close-btn");
    digitalDiv = $("#digital");
});