const fs = require("fs");
exports.imageSize = (p) => { const b = fs.readFileSync(p); return { width: b.readUInt32BE(16), height: b.readUInt32BE(20) }; };
