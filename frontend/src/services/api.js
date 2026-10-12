
import axios from "axios";

const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 15000,
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
    const response = await api.request({
      url: path,
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
              ? "The request timed out."
              : "Cannot connect to the backend. Check that Spring Boot is running.");

    const normalizedError = new Error(message);
    normalizedError.status = status;
    normalizedError.cause = error;

    throw normalizedError;
  }
}

export { api };
export default apiRequest;
