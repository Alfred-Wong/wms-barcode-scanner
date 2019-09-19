const exec = require("cordova/exec");
const channel = require("cordova/channel");
const WMSbarcode = function() {
    let _debug = true;
    this._channels = {};

    this.channelExists = function(c) {
        return this._channels.hasOwnProperty(c);
    };

    this.channelCreate = function(c) {
        if (_debug) console.log("CHANNEL " + c + " CREATED! ");
        this._channels[c] = channel.create(c);
    };
    this.channelSubscribe = function(c, f) {
        var channel = this._channels[c];
        channel.subscribe(f);
        if (_debug) console.log("CHANNEL " + c + " SUBSCRIBED! " + channel.numHandlers);
        return channel.numHandlers;
    };
    this.channelUnsubscribe = function(c, f) {
        var channel = this._channels[c];
        channel.unsubscribe(f);
        if (_debug)
            console.log("CHANNEL " + c + " UNSUBSCRIBED! " + channel.numHandlers);
        return channel.numHandlers;
    };
    this.channelFire = function(event) {
        if (_debug) console.log("CHANNEL " + event.type + " FIRED! ");
        this._channels[event.type].fire(event);
    };
    this.channelDelete = function(c) {
        delete this._channels[c];
        if (_debug) console.log("CHANNEL " + c + " DELETED! ");
    };
};

WMSbarcode.prototype.coolMethod = function(arg0, success, error) {
    exec(success, error, "wmsbarcode", "coolMethod", [arg0]);
};

WMSbarcode.prototype.startScan = function(success, error) {
    exec(success, error, "wmsbarcode", "startScan");
};

WMSbarcode.prototype.stopScan = function(success, error) {
    exec(success, error, "wmsbarcode", "stopScan");
};

WMSbarcode.prototype.addEventListener = function(eventname, callback) {
    if (!this.channelExists(eventname)) {
        this.channelCreate(eventname);
    }
    this.channelSubscribe(eventname, callback);
};

WMSbarcode.prototype.fireEvent = function(type, data) {
    if (!this.channelExists(type)) return;

    const event = document.createEvent("Event");
    event.initEvent(type, false, false);
    if (data) {
        for (let i in data) {
            if (data.hasOwnProperty(i)) {
                event[i] = data[i];
            }
        }
    }
    this.channelFire(event);
};

module.exports = new WMSbarcode();
