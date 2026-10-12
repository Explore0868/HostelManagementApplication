
import axios from "axios";

export function getApiBaseUrl() {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (!envUrl || !envUrl.trim()) {
    return "http://localhost:8080/api";
  }
  const cleanUrl = envUrl.trim().replace(/\/+$/, "");
  return cleanUrl.endsWith("/api") ? cleanUrl : `${cleanUrl}/api`;
}

const api = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 60000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      window.location.pathname !== "/login"
    ) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.assign("/login");
    }

    return Promise.reject(error);
  }
);

export async function apiRequest(path, options = {}) {
  try {
    const normalizedPath = path.startsWith("/api/")
      ? path.replace(/^\/api/, "")
      : path;

    const response = await api.request({
      url: normalizedPath,
      method: options.method || "GET",
      data: options.body,
      headers: options.headers,
    });

    return response.data;
  } catch (error) {
    const status = error.response?.status;
    const data = error.response?.data;

    const message =
      data?.message ||
      data?.detail ||
      (status === 401
        ? "Invalid credentials or session expired."
        : status === 403
          ? "You do not have permission to perform this action."
          : status
            ? `Request failed with status ${status}.`
            : error.code === "ECONNABORTED"
              ? "The request timed out. The backend server might be waking up—please retry in a few moments."
              : "Cannot connect to the backend. Check that the Spring Boot server is running.");

    const normalizedError = new Error(message);
    normalizedError.status = status;
    normalizedError.cause = error;

    throw normalizedError;
  }
}

export { api };
export default apiRequest;
