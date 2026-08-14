const paths: Record<string, React.ReactNode> = {
  dashboard: (
    <path d="M4 13h6V4H4v9zm0 7h6v-4H4v4zm8 0h6v-9h-6v9zm0-16v4h6V4h-6z" />
  ),
  patients: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 4-6 8-6s8 2 8 6" />
    </>
  ),
  doctors: (
    <>
      <circle cx="12" cy="7" r="4" />
      <path d="M4 21c0-4 4-6 8-6s8 2 8 6" />
      <path d="M2 10h3M4 8v2" />
    </>
  ),
  appointments: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18" />
    </>
  ),
  pharmacy: (
    <>
      <path d="M10 2h4" />
      <path d="M10 2v8l-5 9a3 3 0 0 0 2.7 1.5h8.6A3 3 0 0 0 19 19l-5-9V2" />
    </>
  ),
  inventory: (
    <>
      <path d="M21 8l-9-5-9 5v8l9 5 9-5V8z" />
      <path d="M3 8l9 5 9-5M12 13v9" />
    </>
  ),
  pos: (
    <>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M6 9h4" />
    </>
  ),
  sales: (
    <>
      <path d="M6 7h12l2 13H4L6 7z" />
      <path d="M9 7a3 3 0 0 1 6 0" />
    </>
  ),
  lab: (
    <>
      <path d="M9 3h6M10 3v6l-4 9a2 2 0 0 0 1.8 3h8.4a2 2 0 0 0 1.8-3l-4-9V3" />
    </>
  ),
  billing: (
    <>
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <path d="M2 10h20M12 4v16" />
    </>
  ),
  customers: (
    <>
      <circle cx="9" cy="8" r="4" />
      <path d="M2 21c0-3.3 3-5 7-5s7 1.7 7 5" />
      <path d="M17 8c1.8 0 3 1.5 3 3s-1.2 3-3 3" />
    </>
  ),
  suppliers: (
    <>
      <path d="M3 7l9-4 9 4-9 4-9-4z" />
      <path d="M3 7v10l9 4 9-4V7" />
      <path d="M12 11v10" />
    </>
  ),
  users: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-3.3 3-6 8-6s8 2.7 8 6" />
    </>
  ),
  roles: (
    <>
      <circle cx="8" cy="8" r="4" />
      <circle cx="17" cy="9" r="3" />
      <path d="M3 20c0-3 2-5 5-5s5 2 5 5M14 20c0-2 .5-3 1.5-4M19 16.5V20M21 16.5V20" />
    </>
  ),
  branches: (
    <>
      <path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6" />
    </>
  ),
  audit: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </>
  ),
  notifications: (
    <>
      <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6z" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </>
  ),
  profile: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-3.3 3-5 8-5s8 1.7 8 5" />
    </>
  ),
  logout: <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />,
  send: <path d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7z" />,
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 1v3M12 20v3M1 12h3M20 12h3M4 4l2 2M18 18l2 2M20 4l-2 2M6 18l-2 2" />
    </>
  ),
  menu: <path d="M3 6h18M3 12h18M3 18h18" />,
  x: <path d="M18 6 6 18M6 6l12 12" />,
  bell: (
    <>
      <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6z" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </>
  ),
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="M16 16l5 5" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  refresh: (
    <>
      <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
      <path d="M21 3v5h-5" />
      <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
      <path d="M3 21v-5h5" />
    </>
  ),
};

export function Icon({ name, size = 18 }: { name: keyof typeof paths | string; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name] ?? null}
    </svg>
  );
}