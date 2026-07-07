const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const targetDir = path.join(__dirname, 'target');
const jarFiles = fs.existsSync(targetDir)
  ? fs.readdirSync(targetDir).filter((file) => file.endsWith('.jar'))
  : [];

const backendJar = jarFiles.length ? path.join(targetDir, jarFiles[0]) : null;
const env = { ...process.env };

defaultLogger();

if (backendJar) {
  console.log(`Iniciando backend Spring Boot desde: ${backendJar}`);
  const backend = spawn('java', ['-jar', backendJar], { stdio: 'inherit', env });

  backend.on('error', (error) => {
    console.error('Error al iniciar el backend:', error.message);
  });

  backend.on('exit', (code, signal) => {
    if (signal) {
      console.log(`Backend detenido por señal ${signal}`);
    } else {
      console.log(`Backend finalizó con código ${code}`);
    }
  });
} else {
  console.warn('No se encontró ningún JAR en target/.');
  console.warn('Compila primero el backend con Maven para poder arrancarlo automáticamente.');
  console.warn('Ejemplo: mvn package');
}

require('./index.js');

function defaultLogger() {
  console.log('Iniciando frontend...');
  console.log('Si el backend no arranca automáticamente, asegúrate de compilar el JAR en target/.');
}
