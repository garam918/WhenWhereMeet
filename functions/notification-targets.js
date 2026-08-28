"use strict";

function selectLatestNotificationDevice(devices) {
  return devices.reduce((latest, device) => {
    if (!latest) return device;
    const deviceLoginAt = device.lastLoginAt || device.updatedAt || "";
    const latestLoginAt = latest.lastLoginAt || latest.updatedAt || "";
    if (deviceLoginAt !== latestLoginAt) {
      return deviceLoginAt > latestLoginAt ? device : latest;
    }
    return String(device.id || "") > String(latest.id || "") ? device : latest;
  }, null);
}

module.exports = {selectLatestNotificationDevice};
