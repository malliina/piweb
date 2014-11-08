var blasterDiv;
var wheel;

var onStatus = function (status) {
    onColor(status.color);
    onBrightnessChanged(status.brightness);
    $("#colorBody").show();
};
var onOpened = function () {
    blasterDiv.hide();
};
var onClosed = function () {
    blasterDiv.show();
};
var onColor = function (color) {
    var red = color.red;
    var green = color.green;
    var blue = color.blue;
    var hex = Raphael.getRGB("rgb(" + red + "," + green + "," + blue + ")").hex;
    wheel.color(hex);
};
var onHardwarePwm = function (boardNum, value) {

};
var onDigitalStateChanged = function (boardNum, state) {

};
var onBlastChanged = function (boardNum, value) {
    adjustSlider(boardNum, value);
};
var onBrightnessChanged = function (brightness) {
    adjustSlider("brightness", brightness);
};
var onReleased = function (boardNum) {
    adjustSlider(boardNum, 0);
};
var adjustSlider = function (elemID, value) {
    $("#" + elemID).val(value);
};

var lastRgbHex;
var wheelChanged = function (rgb) {
    var rgbHex = rgb.hex;
    if (rgbHex != lastRgbHex) {
        lastRgbHex = rgbHex;
        onRgb(rgb);
    }
};
$(document).ready(function () {
    blasterDiv = $("#blaster");
    wheel = Raphael.colorwheel($(".colorwheel"), 300, 360).color("#FF0000");
    wheel.onchange(wheelChanged);
});