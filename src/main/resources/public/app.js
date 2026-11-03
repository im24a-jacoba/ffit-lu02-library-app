'use strict';
const $ = id => document.getElementById(id);
let books = [], page = 0, token = null;
const pageSize = 12;
const colors = ['#486452', '#ad7758', '#9b9b72', '#607c80', '#b29968'];
function status(id, text, error = false) { $(id).textContent = text; $(id).classList.toggle('error', error); }
async function api(path, options = {}) {
  let response;
  try { response = await fetch(path, options); }
  catch { throw new Error('Keine Verbindung. Bitte prüfe, ob die Anwendung läuft.'); }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(response.status === 401 ? 'E-Mail, Passwort oder Sitzung ungültig. Bitte erneut prüfen.' : response.status === 400 ? 'Bitte prüfe deine Eingaben.' : 'Die Anfrage konnte nicht verarbeitet werden. Bitte später erneut versuchen.');
    error.status = response.status; throw error;
  }
  return data;
}
function textElement(tag, text, className) { const element = document.createElement(tag); element.textContent = text; if (className) element.className = className; return element; }
function render() {
  const query = $('search').value.trim().toLocaleLowerCase('de');
  const filtered = books.filter(book => [book.title, book.author, book.isbn].some(value => String(value ?? '').toLocaleLowerCase('de').includes(query)));
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize)); page = Math.min(page, pages - 1);
  $('count').textContent = `${filtered.length.toLocaleString('de-CH')} Bücher`;
  $('books').replaceChildren();
  for (const book of filtered.slice(page * pageSize, (page + 1) * pageSize)) {
    const card = textElement('article', '', 'book');
    const cover = textElement('div', (book.title || '?').slice(0, 1), 'cover');
    cover.setAttribute('aria-hidden', 'true'); cover.style.backgroundColor = colors[Math.abs(book.id || 0) % colors.length];
    cover.append(textElement('small', String(book.publicationYear ?? 'BUCH')));
    const details = textElement('div', '', 'book-details');
    details.append(textElement('h3', book.title), textElement('p', book.author, 'author'), textElement('div', `ISBN ${book.isbn ?? '–'}`, 'metadata'));
    card.append(cover, details); $('books').append(card);
  }
  $('previous').disabled = page === 0; $('next').disabled = page >= pages - 1;
  $('page').textContent = `Seite ${page + 1} von ${pages}`;
  if (!filtered.length) status('catalog-status', books.length ? 'Keine Bücher für diese Suche gefunden.' : 'Noch keine Bücher vorhanden. Bitte zuerst Bücher importieren.');
  else status('catalog-status', query ? `Suchergebnisse für „${$('search').value.trim()}“` : 'Einfach durch die Sammlung stöbern.');
}
async function loadBooks() {
  $('refresh').disabled = true; status('catalog-status', 'Bücher werden geladen …');
  try { books = await api('/books'); render(); }
  catch (error) { status('catalog-status', error.message, true); $('count').textContent = 'Nicht verfügbar'; }
  finally { $('refresh').disabled = false; }
}
function signedIn(email) {
  $('login-form').hidden = Boolean(token); $('signed-in').hidden = !token;
  $('identity').textContent = email || '';
  $('account-title').textContent = token ? 'Dein Leseraum.' : 'Willkommen zurück.';
  $('account-description').textContent = token ? 'Hier kannst du dein Passwort ändern.' : 'Melde dich an, um dein Passwort zu verwalten.';
}
$('login-form').addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget, button = form.querySelector('button'); button.disabled = true;
  const values = Object.fromEntries(new FormData(form));
  try { const data = await api('/auth/login', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(values)}); token = data.token; signedIn(values.email); form.reset(); status('auth-status', 'Du bist angemeldet.'); }
  catch(error) { status('auth-status', error.message, true); }
  finally { button.disabled = false; }
});
$('password-form').addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget, values = Object.fromEntries(new FormData(form));
  if (values.newPassword !== values.confirmPassword) { status('auth-status', 'Die neuen Passwörter stimmen nicht überein.', true); return; }
  const button = form.querySelector('button'); button.disabled = true;
  try { await api('/auth/change-password', {method:'PUT', headers:{'Content-Type':'application/json', Authorization:`Bearer ${token}`}, body:JSON.stringify({oldPassword:values.oldPassword, newPassword:values.newPassword})}); form.reset(); status('auth-status', 'Dein Passwort wurde geändert.'); }
  catch(error) { status('auth-status', error.message, true); }
  finally { button.disabled = false; }
});
$('logout').addEventListener('click', () => { token = null; $('password-form').reset(); signedIn(); status('auth-status', 'Du bist abgemeldet.'); });
$('search').addEventListener('input', () => { page = 0; render(); });
$('previous').addEventListener('click', () => { page--; render(); });
$('next').addEventListener('click', () => { page++; render(); });
$('refresh').addEventListener('click', loadBooks);
loadBooks();
