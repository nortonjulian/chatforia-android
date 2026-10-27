// Isolated QA only: never targets a physical device, production server or account.
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

const serial = 'emulator-5554';
function adb(args, timeout = 90000) {
  const result = spawnSync('adb', ['-s', serial, ...args], {
    encoding: 'utf8', timeout, maxBuffer: 1024 * 1024,
  });
  if (result.status !== 0 || result.error)
    throw new Error('ADB failed (' + args[0] + '): ' + (result.error?.message || result.status));
  return result.stdout.trim();
}
async function session() {
  const files = [];
  for (const e of await fs.readdir(os.tmpdir(), { withFileTypes: true })) {
    if (!e.isDirectory() || !/^chatforia-private-media-qa-[\w-]+$/.test(e.name)) continue;
    const f = path.join(os.tmpdir(), e.name, 'session.json');
    try {
      const st = await fs.stat(f);
      if (st.isFile() && !(st.mode & 0o077)) files.push(f);
    } catch {}
  }
  if (files.length !== 1) throw new Error('Expected exactly one secure disposable QA session.');
  const s = JSON.parse(await fs.readFile(files[0], 'utf8'));
  if (!s.token?.receiver || !Number.isInteger(s.attachmentId?.image) ||
      !Number.isInteger(s.attachmentId?.audio)) throw new Error('QA session lacks required fixtures.');
  return s;
}

async function main() {
  const response = await fetch('http://127.0.0.1:5502/qa/health', {
    signal: AbortSignal.timeout(5000), redirect: 'manual',
  });
  if (!response.ok || (await response.json()).purpose !== 'isolated-private-media-qa')
    throw new Error('Expected local-only QA API on port 5502.');
  const s = await session();
  if (adb(['shell', 'getprop', 'ro.kernel.qemu']) !== '1' ||
      adb(['shell', 'getprop', 'sys.boot_completed']) !== '1')
    throw new Error('The explicitly targeted emulator is not booted.');
  for (const pkg of ['com.chatforia.android', 'com.chatforia.android.test']) {
    if (adb(['shell', 'pm', 'path', pkg]).includes('package:'))
      throw new Error(pkg + ' is already installed: refusing to overwrite it.');
  }
  const app = 'app/build/outputs/apk/debug/app-debug.apk';
  const tests = 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk';
  await fs.access(app); await fs.access(tests);
  console.log('Isolated localhost API, secure QA session and emulator verified.');
  console.log(adb(['install', '-t', app]));
  console.log(adb(['install', '-t', tests]));
  console.log('Running the native Compose media attachment test (one test)...');
  const args = ['shell', 'am', 'instrument', '-w', '-r',
    '-e', 'class', 'com.chatforia.android.PrivateMediaAttachmentQaTest',
    '-e', 'privateMediaQaToken', s.token.receiver,
    '-e', 'privateMediaQaImageId', String(s.attachmentId.image),
    '-e', 'privateMediaQaAudioId', String(s.attachmentId.audio),
    'com.chatforia.android.test/com.chatforia.android.PrivateMediaQaRunner'];
  const result = spawnSync('adb', ['-s', serial, ...args], {
    encoding: 'utf8', timeout: 180000, maxBuffer: 2 * 1024 * 1024,
  });
  const output = (result.stdout || '') + '\n' + (result.stderr || '');
  const safeOutput = output.replaceAll(s.token.receiver, '[REDACTED]')
    .replace(/private-media\/file\?token=[\w.%_-]+/g, 'private-media/file?token=[REDACTED]');
  process.stdout.write(safeOutput);
  if (result.error || result.status !== 0 || !/OK \(1 test\)/.test(safeOutput))
    throw new Error('Native attachment UI test did not pass.');
  console.log('PASS: native image/audio attachment authorization and UI rendering.');
  console.log('Audible native playback and video remain separate QA checks.');
}
main().catch(e => { console.error('QA stopped:', e.message); process.exitCode = 1; });
