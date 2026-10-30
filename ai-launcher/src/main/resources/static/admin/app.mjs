import { STORAGE_KEY, MAX_PROFILES, ENDPOINTS, EXAMPLE_BODY, validateProfile,
  restoreWorkspace, endpointUrl, prettyBody, curlCommand } from './core.mjs';

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const text = (selector, value) => { $(selector).textContent = value; };
const make = (tag, className, value) => {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (value !== undefined) element.textContent = value;
  return element;
};
let toastTimer;
function notify(message) {
  text('#toast', message);
  $('#toast').hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 5000);
}

let workspace;
try {
  workspace = restoreWorkspace(localStorage.getItem(STORAGE_KEY), location.origin);
} catch {
  workspace = restoreWorkspace(null, location.origin);
  notify('连接档案无法读取，已使用当前服务。新修改会尝试保存到此浏览器。');
}
const health = new Map();
const healthChecks = new Map();
let history = [];
let requestCount = 0;
let lastLatency = null;
let editingId = null;
let deletingId = null;
let pendingRequest = null;
let lastResponse = null;
let responseTab = 'body';
const activeProfile = () => workspace.profiles.find(profile => profile.id === workspace.activeId);

function persist() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(workspace)); }
  catch { notify('浏览器存储不可用，修改仅在当前页面保留。'); }
}

function statusLabel(id) {
  if (healthChecks.has(id)) return '检查中…';
  return health.get(id)?.label ?? '未检查';
}

function statusClass(id) {
  if (healthChecks.has(id)) return '';
  return health.has(id) ? (health.get(id).ok ? 'good' : 'bad') : '';
}

function updateSummary() {
  const profile = activeProfile();
  text('#nav-count', workspace.profiles.length);
  text('#table-count', workspace.profiles.length);
  $('#profile-count').replaceChildren(document.createTextNode(String(workspace.profiles.length)), make('span', '', '个'));
  text('#gateway-status', statusLabel(profile.id));
  $('#gateway-status').className = 'stat-value status-value ' + (statusClass(profile.id) === 'good' ? 'good-text' : statusClass(profile.id) === 'bad' ? 'bad-text' : '');
  text('#gateway-name', profile.name);
  $('#last-latency').replaceChildren(document.createTextNode(lastLatency === null ? '—' : String(lastLatency)), ...(lastLatency === null ? [] : [make('span', '', 'ms')]));
  $('#request-count').replaceChildren(document.createTextNode(String(requestCount)), make('span', '', '次'));
  text('#test-connection-name', profile.name);
  text('#test-connection-url', profile.url);
  $('#check-active').disabled = healthChecks.has(profile.id);
}

function action(label, handler, className = '') {
  const button = make('button', 'text-button ' + className, label);
  button.type = 'button';
  button.addEventListener('click', handler);
  return button;
}

function renderConnections() {
  const rows = workspace.profiles.map(profile => {
    const selected = profile.id === workspace.activeId;
    const row = make('tr');
    const nameCell = make('td');
    const nameWrap = make('div', 'connection-cell');
    const info = make('div');
    const name = make('strong', '', profile.name);
    if (selected) name.append(make('span', 'active-label', '当前连接'));
    info.append(name, make('small', '', selected ? '用于当前 API 测试' : '已保存的网关连接'));
    nameWrap.append(make('span', 'connection-icon', '⇄'), info);
    nameCell.append(nameWrap);
    const urlCell = make('td');
    urlCell.append(make('code', 'url-code', profile.url));
    const statusCell = make('td');
    const status = make('span', 'status-pill ' + statusClass(profile.id), statusLabel(profile.id));
    status.title = health.get(profile.id)?.detail ?? '点击检查以获取实时状态';
    statusCell.append(status);
    const actionsCell = make('td');
    const actions = make('div', 'row-actions');
    const select = action(selected ? '去测试' : '使用', () => {
      workspace.activeId = profile.id;
      persist();
      renderConnections();
      if (selected) location.hash = 'playground';
    });
    const check = action('检查', () => checkHealth(profile.id), 'muted');
    check.disabled = healthChecks.has(profile.id);
    const remove = action('删除', () => confirmDelete(profile.id), 'delete');
    remove.disabled = workspace.profiles.length === 1;
    remove.title = remove.disabled ? '至少保留一个连接' : '删除此连接档案';
    actions.append(select, check, action('编辑', () => openEditor(profile.id), 'muted'), remove);
    actionsCell.append(actions);
    row.append(nameCell, urlCell, statusCell, actionsCell);
    return row;
  });
  $('#connection-rows').replaceChildren(...rows);
  updateSummary();
}

async function checkHealth(id) {
  const profile = workspace.profiles.find(item => item.id === id);
  if (!profile || healthChecks.has(id)) return;
  const originalUrl = profile.url;
  const controller = new AbortController();
  healthChecks.set(id, controller);
  renderConnections();
  const start = performance.now();
  const timeout = setTimeout(() => controller.abort(), 5000);
  let result;
  try {
    const response = await fetch(endpointUrl(originalUrl, ENDPOINTS.health.path), {
      signal: controller.signal, cache: 'no-store', credentials: 'omit', headers: { Accept: 'application/json' },
    });
    const data = await response.json();
    const ok = response.ok && data?.status === 'UP';
    result = { ok, label: ok ? '运行正常' : '状态异常', detail: `HTTP ${response.status} · ${String(data?.status ?? '无健康状态')}` };
  } catch {
    result = { ok: false, label: '无法连接', detail: controller.signal.aborted ? '检查超时（5 秒）' : '请检查服务地址、网络和网关 CORS 配置' };
  } finally {
    clearTimeout(timeout);
    if (healthChecks.get(id) === controller) {
      healthChecks.delete(id);
      if (workspace.profiles.some(item => item.id === id && item.url === originalUrl)) {
        health.set(id, result);
        lastLatency = Math.round(performance.now() - start);
      }
      renderConnections();
    }
  }
}

function cancelHealth(id) {
  healthChecks.get(id)?.abort();
  healthChecks.delete(id);
  health.delete(id);
}

function openEditor(id = null) {
  if (!id && workspace.profiles.length >= MAX_PROFILES) return notify(`最多保存 ${MAX_PROFILES} 个连接。`);
  editingId = id;
  const profile = workspace.profiles.find(item => item.id === id);
  $('#connection-form').reset();
  text('#dialog-title', profile ? '编辑连接' : '新建连接');
  $('#connection-name').value = profile?.name ?? '';
  $('#connection-url').value = profile?.url ?? location.origin;
  $('#connection-error').hidden = true;
  $('#connection-dialog').showModal();
}

$('#connection-form').addEventListener('submit', event => {
  event.preventDefault();
  try {
    const values = validateProfile({ name: $('#connection-name').value, url: $('#connection-url').value });
    if (editingId) {
      const profile = workspace.profiles.find(item => item.id === editingId);
      if (!profile) throw new Error('此连接已不存在，请重新添加。');
      cancelHealth(editingId);
      Object.assign(profile, values);
    } else {
      if (workspace.profiles.length >= MAX_PROFILES) throw new Error(`最多保存 ${MAX_PROFILES} 个连接。`);
      workspace.profiles.push({ id: crypto.randomUUID?.() ?? `gateway-${Date.now()}-${Math.random().toString(36).slice(2)}`, ...values });
    }
    persist();
    renderConnections();
    $('#connection-dialog').close();
  } catch (error) {
    text('#connection-error', error.message);
    $('#connection-error').hidden = false;
  }
});

function confirmDelete(id) {
  if (workspace.profiles.length <= 1) return;
  deletingId = id;
  const profile = workspace.profiles.find(item => item.id === id);
  text('#delete-description', `确定删除“${profile.name}”？`);
  $('#delete-dialog').showModal();
}

$('#delete-form').addEventListener('submit', event => {
  event.preventDefault();
  if (workspace.profiles.length > 1) {
    cancelHealth(deletingId);
    workspace.profiles = workspace.profiles.filter(profile => profile.id !== deletingId);
    if (!workspace.profiles.some(profile => profile.id === workspace.activeId)) workspace.activeId = workspace.profiles[0].id;
    persist();
    renderConnections();
  }
  $('#delete-dialog').close();
});

const views = {
  connections: ['连接管理', '管理网关连接，在一个工作台完成检查与调试。'],
  playground: ['API 测试', '发送真实请求，检查响应、耗时与接口行为。'],
  activity: ['请求记录', '回看本次调试的请求结果，快速定位失败。'],
};
function navigate() {
  const view = Object.hasOwn(views, location.hash.slice(1)) ? location.hash.slice(1) : 'connections';
  $$('[data-view]').forEach(element => { element.hidden = element.dataset.view !== view; });
  $$('[data-nav]').forEach(element => {
    element.classList.toggle('active', element.dataset.nav === view);
    if (element.dataset.nav === view) element.setAttribute('aria-current', 'page');
    else element.removeAttribute('aria-current');
  });
  text('#page-title', views[view][0]);
  text('#breadcrumb-current', views[view][0]);
  text('#page-description', views[view][1]);
  $('#add-connection').hidden = view !== 'connections';
}

function updateEndpoint() {
  const endpoint = ENDPOINTS[$('#endpoint').value];
  text('#request-method', endpoint.method);
  $('#request-method').className = 'method ' + (endpoint.method === 'POST' ? 'post' : '');
  text('#request-path', endpoint.path);
  $('#request-body').disabled = endpoint.method !== 'POST';
  $('#reset-body').hidden = endpoint.method !== 'POST';
  text('#body-help', endpoint.method === 'POST' ? 'model 填写模型目录中的别名（默认 assistant），当前仅支持 user 角色。' : 'GET 请求不发送请求体。');
  $('#request-error').hidden = true;
}

function renderResponse() {
  $$('[data-response-tab]').forEach(button => {
    const selected = button.dataset.responseTab === responseTab;
    button.setAttribute('aria-selected', String(selected));
    button.tabIndex = selected ? 0 : -1;
  });
  $('#response-content').setAttribute('aria-labelledby', responseTab === 'body' ? 'body-tab' : 'headers-tab');
  if (!lastResponse) return;
  $('#empty-response').hidden = true;
  $('#response-content').hidden = false;
  text('#response-content', responseTab === 'body' ? prettyBody(lastResponse.body) : (lastResponse.headers || '(无可读取的响应头)'));
  $('#copy-response').disabled = false;
}

function renderHistory() {
  if (!history.length) {
    const row = make('tr');
    const cell = make('td', 'empty-table', '还没有请求记录。到 API 测试台发送第一条请求。');
    cell.colSpan = 4;
    row.append(cell);
    $('#history-rows').replaceChildren(row);
    return;
  }
  $('#history-rows').replaceChildren(...history.map(entry => {
    const row = make('tr');
    const time = make('td', '', entry.time);
    time.append(make('span', 'history-detail', entry.connection));
    const request = make('td');
    request.append(make('code', 'url-code', `${entry.method} ${entry.path}`));
    const status = make('td');
    status.append(make('span', 'status-pill ' + (entry.ok ? 'good' : 'bad'), entry.status));
    row.append(time, request, status, make('td', 'url-code', `${entry.duration} ms`));
    return row;
  }));
}

$('#request-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (pendingRequest) return;
  $('#request-error').hidden = true;
  const endpoint = ENDPOINTS[$('#endpoint').value];
  const profile = { ...activeProfile() };
  const body = $('#request-body').value;
  if (endpoint.method === 'POST') {
    try { JSON.parse(body); }
    catch {
      text('#request-error', '请求体不是有效 JSON，请检查引号、逗号和括号。');
      $('#request-error').hidden = false;
      $('#request-body').focus();
      return;
    }
  }
  const controller = new AbortController();
  pendingRequest = controller;
  $('#send-request').disabled = true;
  text('#send-request', '请求中…');
  $('#cancel-request').hidden = false;
  text('#response-description', `${profile.name} · ${endpoint.method} ${endpoint.path} · 请求中…`);
  const timeout = setTimeout(() => controller.abort('timeout'), 30000);
  const start = performance.now();
  let result;
  try {
    const response = await fetch(endpointUrl(profile.url, endpoint.path), {
      method: endpoint.method, signal: controller.signal, cache: 'no-store', credentials: 'omit',
      headers: { Accept: 'application/json', ...(endpoint.method === 'POST' ? { 'Content-Type': 'application/json' } : {}) },
      ...(endpoint.method === 'POST' ? { body } : {}),
    });
    const responseBody = await response.text();
    const requestId = response.headers.get('X-Request-Id');
    result = { ok: response.ok, status: String(response.status), body: responseBody,
      headers: [...response.headers].map(([key, value]) => `${key}: ${value}`).join('\n'),
      requestId, size: new Blob([responseBody]).size };
  } catch {
    const message = controller.signal.aborted
      ? (controller.signal.reason === 'timeout' ? '请求超时（30 秒）。' : '请求已取消。')
      : '无法连接网关。请检查服务地址、服务进程、网络及 CORS 配置；HTTPS 页面不能请求 HTTP 地址。';
    result = { ok: false, status: controller.signal.aborted ? (controller.signal.reason === 'timeout' ? '超时' : '已取消') : '网络错误', body: message, headers: '', requestId: null, size: null };
  } finally {
    clearTimeout(timeout);
    pendingRequest = null;
    $('#send-request').disabled = false;
    text('#send-request', '发送请求 ↗');
    $('#cancel-request').hidden = true;
  }
  const duration = Math.round(performance.now() - start);
  lastLatency = duration;
  requestCount += 1;
  lastResponse = result;
  responseTab = 'body';
  text('#response-description', `${profile.name} · ${endpoint.method} ${endpoint.path}`);
  $('#response-meta').hidden = false;
  text('#response-status', result.status);
  $('#response-status').className = 'status-pill ' + (result.ok ? 'good' : 'bad');
  text('#response-duration', `${duration} ms`);
  text('#response-size', result.size === null ? '' : `${result.size} B`);
  $('#request-id').hidden = !result.requestId;
  text('#request-id', result.requestId ? `requestId: ${result.requestId}` : '');
  history.unshift({ connection: profile.name, time: new Date().toLocaleTimeString('zh-CN', { hour12: false }), method: endpoint.method, path: endpoint.path, status: result.status, ok: result.ok, duration });
  history = history.slice(0, 20);
  updateSummary();
  renderResponse();
  renderHistory();
});

async function copy(value, label) {
  try {
    await navigator.clipboard.writeText(value);
    notify(`${label}已复制`);
  } catch { notify('浏览器未允许剪贴板访问，请手动选择并复制。'); }
}

$('#add-connection').addEventListener('click', () => openEditor());
$('#check-active').addEventListener('click', () => checkHealth(workspace.activeId));
$('#close-dialog').addEventListener('click', () => $('#connection-dialog').close());
$('#cancel-dialog').addEventListener('click', () => $('#connection-dialog').close());
$('#cancel-delete').addEventListener('click', () => $('#delete-dialog').close());
$('#endpoint').addEventListener('change', updateEndpoint);
$('#reset-body').addEventListener('click', () => { $('#request-body').value = EXAMPLE_BODY; $('#request-error').hidden = true; });
$('#cancel-request').addEventListener('click', () => pendingRequest?.abort('cancelled'));
$('#copy-curl').addEventListener('click', () => copy(curlCommand(activeProfile().url, ENDPOINTS[$('#endpoint').value], $('#request-body').value), 'cURL'));
$('#copy-response').addEventListener('click', () => { if (lastResponse) copy($('#response-content').textContent, '响应'); });
$$('[data-response-tab]').forEach(button => button.addEventListener('click', () => { responseTab = button.dataset.responseTab; renderResponse(); }));
$('.response-tabs').addEventListener('keydown', event => {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  responseTab = event.key === 'Home' ? 'body' : event.key === 'End' ? 'headers' : responseTab === 'body' ? 'headers' : 'body';
  renderResponse();
  $(`[data-response-tab="${responseTab}"]`).focus();
});
$('#clear-history').addEventListener('click', () => { history = []; renderHistory(); });
window.addEventListener('hashchange', navigate);
$('#request-body').value = EXAMPLE_BODY;
navigate();
renderConnections();
renderHistory();
updateEndpoint();
checkHealth(workspace.activeId);
