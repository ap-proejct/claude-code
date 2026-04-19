window.apiFetch = async (url, method = 'GET', body) => {
  const opts = { url, method, processData: false };
  if (body !== undefined) {
    opts.contentType = 'application/json';
    opts.data = JSON.stringify(body);
  }
  try {
    const data = await $.ajax(opts);
    return { ok: true, json: async () => data };
  } catch (xhr) {
    return { ok: false, json: async () => xhr.responseJSON ?? {} };
  }
};

function openModal(id)  { $('#' + id).removeClass('hidden'); }
function closeModal(id) { $('#' + id).addClass('hidden'); }
