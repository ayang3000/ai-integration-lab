export const STORAGE_KEY = 'ai-lab.gateway-workspace.v1';
export const MAX_PROFILES = 20;
export const ENDPOINTS = {
  chat: { method: 'POST', path: '/api/chat' },
  models: { method: 'GET', path: '/api/models' },
  health: { method: 'GET', path: '/actuator/health' },
  liveness: { method: 'GET', path: '/actuator/health/liveness' },
  readiness: { method: 'GET', path: '/actuator/health/readiness' },
  info: { method: 'GET', path: '/actuator/info' },
  metrics: { method: 'GET', path: '/actuator/metrics' },
};

export const EXAMPLE_BODY = JSON.stringify({
  model: 'assistant',
  messages: [{ role: 'user', content: '你好，请介绍一下你自己。' }],
}, null, 2);

export function normalizeBaseUrl(value) {
  let url;
  try {
    url = new URL(value.trim());
  } catch {
    throw new Error('请输入完整的 HTTP / HTTPS 网关地址。');
  }
  if (!['http:', 'https:'].includes(url.protocol) || !url.hostname
      || url.username || url.password || url.search || url.hash) {
    throw new Error('网关地址仅支持 HTTP / HTTPS，不能包含凭据、查询参数或片段。');
  }
  if (url.href.length > 2048) throw new Error('网关地址过长。');
  return url.href.replace(/\/+$/, '');
}

export function validateProfile(profile) {
  if (typeof profile?.name !== 'string' || !profile.name.trim() || profile.name.trim().length > 60) {
    throw new Error('连接名称需为 1–60 个字符。');
  }
  if (typeof profile.url !== 'string') throw new Error('请输入网关地址。');
  return { name: profile.name.trim(), url: normalizeBaseUrl(profile.url) };
}

export function restoreWorkspace(raw, origin) {
  if (raw === null) {
    return { version: 1, activeId: 'current', profiles: [{ id: 'current', name: '当前服务', url: normalizeBaseUrl(origin) }] };
  }
  const data = JSON.parse(raw);
  if (data?.version !== 1 || !Array.isArray(data.profiles) || !data.profiles.length
      || data.profiles.length > MAX_PROFILES) throw new Error('连接档案格式不正确。');
  const ids = new Set();
  const profiles = data.profiles.map(profile => {
    if (typeof profile?.id !== 'string' || !profile.id || profile.id.length > 64 || ids.has(profile.id)) {
      throw new Error('连接档案标识不正确。');
    }
    ids.add(profile.id);
    return { id: profile.id, ...validateProfile(profile) };
  });
  return { version: 1, profiles, activeId: ids.has(data.activeId) ? data.activeId : profiles[0].id };
}

export function endpointUrl(base, path) {
  return normalizeBaseUrl(base) + path;
}

export function prettyBody(text) {
  try { return JSON.stringify(JSON.parse(text), null, 2); }
  catch { return text || '(空响应)'; }
}

function shellQuote(value) {
  return "'" + value.replaceAll("'", "'\\''") + "'";
}

export function curlCommand(base, endpoint, body) {
  const args = ['curl', '-i', '-X', endpoint.method, shellQuote(endpointUrl(base, endpoint.path))];
  if (endpoint.method === 'POST') args.push('-H', "'Content-Type: application/json'", '--data-raw', shellQuote(body));
  return args.join(' ');
}
