export function clearAuthSession() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  sessionStorage.clear();
  window.history.replaceState(null, "", "/login");
  window.location.replace("/login");
}
