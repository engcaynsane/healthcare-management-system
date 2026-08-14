import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Icon } from "./icons";
import { Modal } from "./Modal";
import { useAuthStore } from "../stores/auth";
import { notificationApi, userApi } from "../lib/api-endpoints";
import { can } from "../lib/permissions";
import type { NotificationItem, User } from "../lib/types";
import { toast } from "../stores/toast";

export function Topbar({ onMenu }: { onMenu: () => void }) {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const [theme, setTheme] = useState(() => localStorage.getItem("theme") || "light");
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifs, setNotifs] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const [profileOpen, setProfileOpen] = useState(false);
  const [sendOpen, setSendOpen] = useState(false);
  const bellRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
  }, [theme]);

  useEffect(() => {
    if (!user) return;
    loadNotifs();
    const id = setInterval(loadNotifs, 60000);
    return () => clearInterval(id);
  }, [user]);

  function loadNotifs() {
    notificationApi
      .list(0, 15)
      .then((p) => {
        setNotifs(p.content);
        setUnread(p.content.filter((n) => !n.read).length);
      })
      .catch(() => {});
  }

  function markAll() {
    notificationApi.markAllRead().then(() => {
      setUnread(0);
      setNotifs((prev) => prev.map((n) => ({ ...n, read: true })));
      toast.success("All notifications marked as read");
    });
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <header className="topbar">
      <button className="btn btn-ghost btn-icon menu-toggle" onClick={onMenu} aria-label="Menu">
        <Icon name="menu" />
      </button>

      <div className="menu" ref={bellRef} style={{ position: "relative" }}>
        <button
          className="btn btn-ghost btn-icon"
          onClick={() => setNotifOpen((v) => !v)}
          aria-label="Notifications"
        >
          <Icon name="bell" />
          {unread > 0 && (
            <span className="badge badge-danger" style={{ position: "absolute", top: 0, right: 0, minWidth: 18, justifyContent: "center" }}>
              {unread}
            </span>
          )}
        </button>
        {notifOpen && (
          <div className="menu-pop" style={{ width: 320, left: 0, right: "auto" }}>
            <div style={{ padding: "10px 12px", borderBottom: "1px solid var(--border)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <strong>Notifications</strong>
              <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                {can("notification.send") && (
                  <button className="btn btn-sm" onClick={() => { setNotifOpen(false); setSendOpen(true); }}>
                    <Icon name="send" /> Send
                  </button>
                )}
                <button className="btn btn-sm btn-ghost" onClick={markAll}>
                  Mark all read
                </button>
              </div>
            </div>
            <div style={{ maxHeight: 320, overflowY: "auto" }}>
              {notifs.length === 0 ? (
                <div className="empty">No notifications</div>
              ) : (
                notifs.map((n) => (
                  <div key={n.id} className="menu-item" style={{ alignItems: "flex-start", flexWrap: "wrap" }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 500, display: "flex", gap: 6, alignItems: "center" }}>
                        {n.title}
                        {!n.read && <span className="badge badge-info">new</span>}
                      </div>
                      <div style={{ fontSize: 12, color: "var(--text-muted)" }}>{n.message}</div>
                      <div style={{ fontSize: 11, color: "var(--text-muted)" }}>{new Date(n.createdAt).toLocaleString()}</div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </div>

      <button
        className="btn btn-ghost btn-icon"
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        title="Toggle theme"
      >
        <Icon name="sun" />
      </button>

      <div className="topbar-spacer" />

      <div className="menu" style={{ position: "relative" }}>
        <button className="btn btn-ghost" onClick={() => setProfileOpen((v) => !v)} style={{ gap: 8, fontWeight: 600 }}>
          <span className="avatar">{initials(user?.fullName)}</span>
          <span className="user-name">{user?.fullName}</span>
        </button>
        {profileOpen && (
          <div className="menu-pop">
            <div style={{ padding: "10px 12px", borderBottom: "1px solid var(--border)" }}>
              <div style={{ fontWeight: 600 }}>{user?.fullName}</div>
              <div style={{ fontSize: 12, color: "var(--text-muted)" }}>@{user?.username}</div>
            </div>
            <Link className="menu-item" to="/profile" onClick={() => setProfileOpen(false)}>
              <Icon name="profile" /> My Profile
            </Link>
            <div className="menu-item" onClick={handleLogout}>
              <Icon name="logout" /> Sign out
            </div>
          </div>
        )}
      </div>

      <SendNotificationModal
        open={sendOpen}
        onClose={() => setSendOpen(false)}
        onSent={loadNotifs}
      />
    </header>
  );
}

function initials(name?: string): string {
  if (!name) return "U";
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}

function SendNotificationModal({ open, onClose, onSent }: { open: boolean; onClose: () => void; onSent: () => void }) {
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [recipientId, setRecipientId] = useState("");
  const [users, setUsers] = useState<User[]>([]);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (open) {
      setTitle("");
      setMessage("");
      setRecipientId("");
      userApi.list({ page: 0, size: 200 }).then((p) => setUsers(p.content)).catch(() => setUsers([]));
    }
  }, [open]);

  function submit() {
    if (!title.trim() || !message.trim()) {
      toast.error("Title and message are required");
      return;
    }
    setSending(true);
    notificationApi
      .send({
        title: title.trim(),
        message: message.trim(),
        type: "MANUAL",
        userId: recipientId ? Number(recipientId) : undefined,
      })
      .then(() => {
        toast.success("Notification sent");
        onSent();
        onClose();
      })
      .catch((e: Error) => toast.error(e.message))
      .finally(() => setSending(false));
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Send Notification"
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={sending} onClick={submit}>
            {sending ? "Sending…" : "Send"}
          </button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field span-2">
          <label>Title <span className="req">*</span></label>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Staff meeting" />
        </div>
        <div className="field span-2">
          <label>Message <span className="req">*</span></label>
          <textarea className="input" rows={3} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Type your message…" />
        </div>
        <div className="field span-2">
          <label>Audience</label>
          <select className="select" value={recipientId} onChange={(e) => setRecipientId(e.target.value)}>
            <option value="">All staff in this branch</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>{u.fullName} (@{u.username})</option>
            ))}
          </select>
        </div>
      </div>
    </Modal>
  );
}