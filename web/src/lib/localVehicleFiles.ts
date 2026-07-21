const DB_NAME = "zellu-vehicle-files";
const STORE_NAME = "files";
const DB_VERSION = 1;

type StoredFile = {
  id: string;
  name: string;
  type: string;
  size: number;
  blob: Blob;
  savedAt: number;
};

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: "id" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveLocalVehicleFile(file: File): Promise<string> {
  const db = await openDb();
  const id = crypto.randomUUID();
  const payload: StoredFile = {
    id,
    name: file.name,
    type: file.type,
    size: file.size,
    blob: file,
    savedAt: Date.now(),
  };
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).put(payload);
    transaction.oncomplete = () => resolve(id);
    transaction.onerror = () => reject(transaction.error);
  });
}

async function getStoredFile(id: string): Promise<StoredFile | undefined> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, "readonly");
    const request = transaction.objectStore(STORE_NAME).get(id);
    request.onsuccess = () => resolve(request.result as StoredFile | undefined);
    request.onerror = () => reject(request.error);
  });
}

export async function openLocalVehicleFile(id: string): Promise<boolean> {
  const file = await getStoredFile(id);
  if (!file) return false;
  const url = URL.createObjectURL(file.blob);
  window.open(url, "_blank", "noopener,noreferrer");
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
  return true;
}

export async function getLocalVehicleFileUrl(id: string): Promise<{ url: string; type: string } | null> {
  const file = await getStoredFile(id);
  if (!file) return null;
  return { url: URL.createObjectURL(file.blob), type: file.type };
}
