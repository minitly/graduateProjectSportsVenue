import { fetchImageByPathApi } from "../api/venue";

const imageCache = new Map();

function toDataUrl(contentType, content) {
  return `data:${contentType};base64,${content}`;
}

function safeFileName(input) {
  return String(input).replace(/[^\w.-]/g, "_");
}

function saveBase64ToTempFile(path, image) {
  return new Promise((resolve) => {
    try {
      if (typeof wx === "undefined" || !wx.getFileSystemManager || !wx.env?.USER_DATA_PATH) {
        resolve("");
        return;
      }
      const fs = wx.getFileSystemManager();
      const ext = (image.contentType || "image/png").split("/")[1] || "png";
      const filePath = `${wx.env.USER_DATA_PATH}/thumb_${safeFileName(path)}.${ext}`;
      fs.writeFile({
        filePath,
        data: image.content,
        encoding: "base64",
        success: () => resolve(filePath),
        fail: () => resolve("")
      });
    } catch (_) {
      resolve("");
    }
  });
}

export async function resolveImagePath(path, options = {}) {
  const preferTempFile = Boolean(options.preferTempFile);
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
    return path;
  }
  const cacheKey = `${path}__${preferTempFile ? "temp" : "data"}`;
  if (imageCache.has(cacheKey)) return imageCache.get(cacheKey);

  const res = await fetchImageByPathApi(path);
  if (res.code !== 200 || !res.data?.images?.length) {
    return "";
  }

  const img = res.data.images[0];
  const tempPath = preferTempFile ? await saveBase64ToTempFile(path, img) : "";
  const result = tempPath || toDataUrl(img.contentType, img.content);
  imageCache.set(cacheKey, result);
  return result;
}

export async function resolveImagePaths(paths, options = {}) {
  const preferTempFile = Boolean(options.preferTempFile);
  const concurrency = Number(options.concurrency || 2);
  const validPaths = (paths || []).filter(Boolean);
  if (!validPaths.length) return {};

  let index = 0;
  async function worker() {
    while (index < validPaths.length) {
      const path = validPaths[index++];
      const cacheKey = `${path}__${preferTempFile ? "temp" : "data"}`;
      if (imageCache.has(cacheKey) || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
        continue;
      }
      const res = await fetchImageByPathApi(path);
      if (res.code === 200 && res.data?.images?.length) {
        const image = res.data.images[0];
        const tempPath = preferTempFile ? await saveBase64ToTempFile(path, image) : "";
        imageCache.set(cacheKey, tempPath || toDataUrl(image.contentType, image.content));
      } else {
        imageCache.set(cacheKey, "");
      }
    }
  }

  const tasks = Array.from({ length: Math.max(1, concurrency) }, () => worker());
  await Promise.all(tasks);

  return validPaths.reduce((acc, p) => {
    const cacheKey = `${p}__${preferTempFile ? "temp" : "data"}`;
    acc[p] = imageCache.get(cacheKey) || (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("data:") ? p : "");
    return acc;
  }, {});
}

