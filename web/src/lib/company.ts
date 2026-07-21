import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb } from "../firebase";
import type { Company } from "../types";

export function emailKey(email: string): string {
  return email.trim().toLowerCase().replace(/[^a-z0-9._-]/g, "_");
}

export async function ensureCompanyForUser(user: User): Promise<Company> {
  const db = getFirebaseDb();
  const userRef = doc(db, "users", user.uid);
  const fallbackCompany: Company = {
    id: `personal_${user.uid}`,
    name: user.displayName ? `Frota de ${user.displayName}` : "Minha frota",
    plan: "frota",
  };

  let activeCompanyId: string | undefined;
  try {
    const userSnap = await getDoc(userRef);
    activeCompanyId = userSnap.data()?.activeCompanyId as string | undefined;
  } catch {
    return fallbackCompany;
  }

  const normalizedEmail = user.email?.trim().toLowerCase();
  if (normalizedEmail && (!activeCompanyId || activeCompanyId.startsWith("personal_"))) {
    try {
      const inviteSnap = await getDocs(query(collection(db, "userInvites", emailKey(normalizedEmail), "companies"), limit(1)));
      const invite = inviteSnap.docs[0];
      if (invite) {
        const inviteData = invite.data();
        const invitedCompanyId = String(inviteData.companyId || invite.id);
        const invitedCompanyName = String(inviteData.companyName || "Empresa");
        await setDoc(
          doc(db, "companies", invitedCompanyId, "members", user.uid),
          {
            uid: user.uid,
            email: normalizedEmail,
            name: user.displayName || normalizedEmail,
            role: inviteData.role || "motorista",
            active: true,
            acceptedAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        await setDoc(
          userRef,
          {
            email: normalizedEmail,
            displayName: user.displayName || "",
            activeCompanyId: invitedCompanyId,
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        return { id: invitedCompanyId, name: invitedCompanyName, plan: "frota" };
      }
    } catch {
      if (!activeCompanyId) return fallbackCompany;
    }
  }

  if (activeCompanyId) {
    try {
      const companySnap = await getDoc(doc(db, "companies", activeCompanyId));
      if (companySnap.exists()) {
        return {
          id: companySnap.id,
          name: String(companySnap.data().name || "Empresa"),
          plan: companySnap.data().plan,
          ownerUid: companySnap.data().ownerUid,
          publicCalendarToken: companySnap.data().publicCalendarToken,
          publicCalendarEnabled: companySnap.data().publicCalendarEnabled,
          speedLimitKmh: Number(companySnap.data().speedLimitKmh ?? 100),
          speedToleranceKmh: Number(companySnap.data().speedToleranceKmh ?? 10),
          speedMinimumSeconds: Number(companySnap.data().speedMinimumSeconds ?? 15),
        };
      }
    } catch {
      return { ...fallbackCompany, id: activeCompanyId };
    }
  }

  const companyId = fallbackCompany.id;
  const displayName = user.displayName || user.email || "Minha empresa";
  const companyRef = doc(db, "companies", companyId);
  try {
    await setDoc(
      companyRef,
      {
        name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`,
        ownerUid: user.uid,
        plan: "frota",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      doc(db, "companies", companyId, "members", user.uid),
      {
        uid: user.uid,
        name: user.displayName || "",
        email: user.email || "",
        role: "administrador",
        active: true,
        createdAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      userRef,
      {
        email: user.email || "",
        displayName: user.displayName || "",
        activeCompanyId: companyId,
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
  } catch {
    return fallbackCompany;
  }

  return { id: companyId, name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`, plan: "frota", ownerUid: user.uid };
}
