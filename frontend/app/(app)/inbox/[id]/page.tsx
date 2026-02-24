"use client";

import { useEffect } from "react";
import { useRouter, useParams } from "next/navigation";

export default function InboxDetailRedirect() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;

  useEffect(() => {
    if (id) {
      router.replace(`/inbox?id=${id}`);
    } else {
      router.replace("/inbox");
    }
  }, [id, router]);

  return null;
}
