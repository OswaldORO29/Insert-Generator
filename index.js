const express = require('express');
const path = require('path');
const http = require('http');
const https = require('https');

const app = express();
const PORT = process.env.PORT || 3000;
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

app.use(express.static(path.join(__dirname, 'src/main/resources/static')));

function proxyToBackend(req, res) {
  const backend = new URL(BACKEND_URL);
  const options = {
    protocol: backend.protocol,
    hostname: backend.hostname,
    port: backend.port || (backend.protocol === 'https:' ? 443 : 80),
    path: req.originalUrl,
    method: req.method,
    headers: { ...req.headers, host: backend.host }
  };

  const client = backend.protocol === 'https:' ? https : http;
  const proxyReq = client.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (error) => {
    res.status(502).json({
      error: 'No se pudo contactar con el backend',
      details: error.message
    });
  });

  if (req.method !== 'GET' && req.method !== 'HEAD') {
    req.pipe(proxyReq);
  } else {
    proxyReq.end();
  }
}

app.use('/api', proxyToBackend);

app.use((req, res, next) => {
  if (req.path.startsWith('/api')) {
    return next();
  }

  if (req.path.includes('.')) {
    return next();
  }

  res.sendFile(path.join(__dirname, 'src/main/resources/static/index.html'));
});

app.listen(PORT, () => {
  console.log(`Frontend corriendo en http://localhost:${PORT}`);
  console.log(`Proxy hacia el backend: ${BACKEND_URL}`);
});
