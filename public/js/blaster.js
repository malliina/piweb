var blasterDiv;
var onOpened = function () {
    blasterDiv.hide();
};
var onClosed = function () {
    blasterDiv.show();
};
var onHardwarePwm = function (boardNum, value) {

};
var onDigitalStateChanged = function (boardNum, state) {

};
var onBlastChanged = function (boardNum, value) {
    $("#" + boardNum).val(value);
};
var onReleased = function (boardNum) {
    $("#" + boardNum).val(0);
};
$(document).ready(function () {
    blasterDiv = $("#blaster");
});