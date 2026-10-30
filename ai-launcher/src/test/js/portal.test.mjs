import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeBaseUrl, validateProfile, restoreWorkspace, endpointUrl, prettyBody, curlCommand, ENDPOINTS } from '../../main/resources/static/admin/core.mjs';

test('gateway URLs retain a path prefix and reject credentials and non-HTTP schemes', () => {
  assert.equal(normalizeBaseUrl(' http://localhost:8080/gateway/ '), 'http://localhost:8080/gateway');
  assert.equal(endpointUrl('https://example.com/gateway/', ENDPOINTS.chat.path), 'https://example.com/gateway/api/chat');
  for (const url of ['javascript:alert(1)', 'file:///tmp/test', 'ftp://localhost', 'http://user:secret@localhost', 'http://localhost?key=secret', 'http://localhost#test', 'not a URL']) {
    assert.throws(() => normalizeBaseUrl(url), Error, url);
  }
});

test('connection form validates names and normalizes saved values', () => {
  assert.deepEqual(validateProfile({ name: '  本地开发  ', url: 'http://localhost:8080/' }), { name: '本地开发', url: 'http://localhost:8080' });
  for (const profile of [{ name: ' ', url: 'http://localhost' }, { name: 'x'.repeat(61), url: 'http://localhost' }, { name: 'local', url: null }]) {
    assert.throws(() => validateProfile(profile));
  }
});

test('new browser starts on the actual serving origin', () => {
  const workspace = restoreWorkspace(null, 'http://127.0.0.1:18080');
  assert.equal(workspace.profiles[0].url, 'http://127.0.0.1:18080');
  assert.equal(workspace.activeId, workspace.profiles[0].id);
});

test('saved profiles round-trip and a deleted active ID falls back to the first profile', () => {
  const workspace = { version: 1, activeId: 'b', profiles: [{ id: 'a', name: '开发', url: 'http://localhost:8080' }, { id: 'b', name: '测试', url: 'https://example.com' }] };
  assert.deepEqual(restoreWorkspace(JSON.stringify(workspace), 'http://localhost'), workspace);
  workspace.activeId = 'deleted';
  assert.equal(restoreWorkspace(JSON.stringify(workspace), 'http://localhost').activeId, 'a');
});

test('corrupt or unsafe browser storage is rejected instead of trusted', () => {
  const profile = { id: 'a', name: 'test', url: 'http://localhost' };
  for (const value of ['{', 'null', JSON.stringify({ version: 1, profiles: [] }),
    JSON.stringify({ version: 1, profiles: [profile, profile] }),
    JSON.stringify({ version: 1, profiles: [{ ...profile, url: 'javascript:alert(1)' }] }),
    JSON.stringify({ version: 1, profiles: Array.from({ length: 21 }, (_, i) => ({ ...profile, id: String(i) })) })]) {
    assert.throws(() => restoreWorkspace(value, 'http://localhost'));
  }
});

test('response viewer handles JSON, plain text and empty responses', () => {
  assert.equal(prettyBody('{"status":"UP"}'), '{\n  "status": "UP"\n}');
  assert.equal(prettyBody('<h1>not json</h1>'), '<h1>not json</h1>');
  assert.equal(prettyBody(''), '(空响应)');
});

test('curl export quotes shell metacharacters and omits a body for GET', () => {
  const body = '{"content":"it\'s `literal` $(literal)"}';
  const command = curlCommand('http://localhost:8080', ENDPOINTS.chat, body);
  assert.ok(command.includes("it'\\''s"));
  assert.ok(command.includes('`literal` $(literal)'));
  assert.ok(command.includes('--data-raw'));
  assert.equal(curlCommand('http://localhost:8080', ENDPOINTS.health, body), "curl -i -X GET 'http://localhost:8080/actuator/health'");
});
