const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');
const fs = require('fs')

const app = express();
let proxy = createProxyMiddleware({
    target: process.env.BACKEND_DOMAIN,
    changeOrigin: true
});

app.use('/api', proxy);
app.use('/static', express.static(path.join(__dirname, 'dist')));
app.use('/submodules', express.static(path.join(__dirname, '../web-submodules/dist')));
app.use('/customtabs/components', express.static(path.join(__dirname, '../web-submodules/dist/tools')));

const mainHtmlFile = path.resolve("/tmp", 'main-dev.html');

fs.readFile(path.resolve(__dirname, 'dist', 'main.html'), 'utf8', function (err, data) {
    if (err) {
        return console.log(err);
    }
    const result = data.replace(/__publicPath__/g, '');

    fs.writeFile(mainHtmlFile, result, 'utf8', function (err) {
        if (err) return console.log(err);
    });
});

app.use((req, res, next) => {
    console.log("Request proxy:", req.path)
    if (req.path.startsWith('/static')) {
      proxy(req, res, next)
    } else {
      res.sendFile(mainHtmlFile);
    }
});

app.listen(3000);
