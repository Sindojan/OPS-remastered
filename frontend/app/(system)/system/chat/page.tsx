"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function SystemChatRedirect() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/system/agents");
  }, [router]);

  return null;
}
