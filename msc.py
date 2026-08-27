#!/usr/bin/env python3
"""ManageSC CLI - alat manajemen penyewaan VPS via terminal.

Replikasi fungsi dari repo bowowiwendi/ManageSC (Google Apps Script / Next.js)
sebagai CLI mandiri (Python stdlib only). Data disimpan di data/vps.json.
"""
import argparse
import json
import os
import sys
import urllib.request
import urllib.error
from datetime import datetime, timedelta, date

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
VPS_FILE = os.path.join(DATA_DIR, "vps.json")
PIN_FILE = os.path.join(DATA_DIR, "pin.json")
GH_FILE = os.path.join(DATA_DIR, "github.json")


# ---------------------------------------------------------------------------
# Storage helpers
# ---------------------------------------------------------------------------
def _ensure_dir():
    os.makedirs(DATA_DIR, exist_ok=True)


def load_vps():
    _ensure_dir()
    try:
        with open(VPS_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_vps(data):
    _ensure_dir()
    with open(VPS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def load_github():
    try:
        with open(GH_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def save_github(cfg):
    _ensure_dir()
    with open(GH_FILE, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------------------
# Date helpers
# ---------------------------------------------------------------------------
def parse_date(s):
    try:
        return datetime.strptime(s, "%Y-%m-%d")
    except (ValueError, TypeError):
        return None


def today():
    return date.today()


def fmt_date(d):
    return d.strftime("%Y-%m-%d")


# ---------------------------------------------------------------------------
# Core operations
# ---------------------------------------------------------------------------
SETUP_SCRIPT = (
    "sysctl net.ipv6.conf.all.disable_ipv6=1 && "
    "sysctl net.ipv6.conf.default.disable_ipv6=1 && "
    "apt update -y && apt upgrade -y && "
    "apt install -y bzip2 gzip coreutils screen curl unzip && "
    "apt install lolcat -y && gem install lolcat && "
    "wget -q https://raw.githubusercontent.com/bowowiwendi/WendyVpn/ABSTRAK/setup-main.sh && "
    "chmod +x setup-main.sh && sed -i -e 's/\\$//' setup-main.sh && "
    "screen -S setupku ./setup-main.sh"
)


def add_entry(username, tipe, masa_aktif, ip, email, ram, pesan, ssh_user="", ssh_pass="", server_aktif=True):
    data = load_vps()
    item = {
        "id": str(int(datetime.now().timestamp() * 1000)),
        "username": username,
        "tipeAkun": tipe,
        "ipVps": ip if ip.startswith("'") else "'" + ip,
        "emailMember": email,
        "ram": ram,
        "pesan": pesan,
        "userSsh": ssh_user,
        "passSsh": ssh_pass,
        "serverAktif": server_aktif,
    }
    if tipe == "limit":
        if masa_aktif.isdigit():
            expiry = today() + timedelta(days=int(masa_aktif))
            item["masaAktif"] = fmt_date(expiry)
        else:
            item["masaAktif"] = masa_aktif
    else:
        item["masaAktif"] = "lifetime"
    data.append(item)
    save_vps(data)
    return item


def update_entry(item_id, **fields):
    item_id = resolve_id(item_id)
    if not item_id:
        return None
    data = load_vps()
    for idx, it in enumerate(data):
        if it["id"] == item_id:
            if "ipVps" in fields and fields["ipVps"] is not None:
                fields["ipVps"] = fields["ipVps"] if fields["ipVps"].startswith("'") else "'" + fields["ipVps"]
            if "masaAktif" in fields and fields["masaAktif"] is not None:
                if fields["masaAktif"].isdigit():
                    fields["masaAktif"] = fmt_date(today() + timedelta(days=int(fields["masaAktif"])))
            if "serverAktif" in fields and fields["serverAktif"] is not None:
                fields["serverAktif"] = str(fields["serverAktif"]).lower() in ("1", "true", "yes")
            data[idx] = {**it, **{k: v for k, v in fields.items() if v is not None}}
            save_vps(data)
            return data[idx]
    return None


def resolve_id(token):
    """Cari id penuh dari id persis, awalan id, atau username."""
    data = load_vps()
    token = str(token)
    for it in data:
        if it["id"] == token:
            return it["id"]
    for it in data:
        if it["id"].startswith(token) or it["username"].lower() == token.lower():
            return it["id"]
    return None


def delete_entry(item_id):
    item_id = resolve_id(item_id)
    if not item_id:
        return False
    data = load_vps()
    new = [it for it in data if it["id"] != item_id]
    if len(new) == len(data):
        return False
    save_vps(new)
    return True


def renew_entry(item_id, days):
    item_id = resolve_id(item_id)
    if not item_id:
        return None
    data = load_vps()
    for idx, it in enumerate(data):
        if it["id"] == item_id:
            expiry = today() + timedelta(days=days)
            data[idx]["masaAktif"] = fmt_date(expiry)
            data[idx]["tipeAkun"] = "limit"
            save_vps(data)
            return data[idx]
    return None


def filter_data(data, search=None, start=None, end=None):
    if start or end:
        sd = parse_date(start) if start else None
        ed = parse_date(end) if end else None
        data = [it for it in data if _in_range(it.get("masaAktif"), sd, ed)]
    if search:
        q = search.lower()
        data = [it for it in data if q in it.get("username", "").lower() or q in it.get("ipVps", "").lower()]
    return data


def _in_range(masa, sd, ed):
    if not masa or masa == "lifetime":
        return True
    d = parse_date(masa)
    if not d:
        return True
    d = d.date()
    if sd and d < sd.date():
        return False
    if ed and d > ed.date():
        return False
    return True


def stats(data):
    return {
        "totalItems": len(data),
        "activeLimited": sum(1 for it in data if it.get("tipeAkun") == "limit"),
        "unlimited": sum(1 for it in data if it.get("tipeAkun") == "unli"),
    }


def check_expiry(days=7):
    data = load_vps()
    result = {"expired": [], "warning": []}
    for it in data:
        masa = it.get("masaAktif")
        if not masa or masa == "lifetime":
            continue
        d = parse_date(masa)
        if not d:
            continue
        delta = (d.date() - today()).days
        if delta < 0:
            result["expired"].append(it)
        elif delta <= days:
            result["warning"].append((it, delta))
    return result


# ---------------------------------------------------------------------------
# GitHub helpers
# ---------------------------------------------------------------------------
def github_content(data):
    lines = []
    for it in data:
        if it.get("username") and it.get("ipVps"):
            ip = it["ipVps"][1:] if it["ipVps"].startswith("'") else it["ipVps"]
            expiry = it.get("masaAktif") or "lifetime"
            lines.append(f"### {it['username']} {expiry} {ip}")
    return "\n".join(lines) + ("\n" if lines else "")


def github_import():
    cfg = load_github()
    if not cfg or not cfg.get("username") or not cfg.get("repo") or not cfg.get("filePath"):
        return {"success": False, "message": "GitHub belum dikonfigurasi. Jalankan `msc github config`."}
    url = f"https://raw.githubusercontent.com/{cfg['username']}/{cfg['repo']}/refs/heads/{cfg.get('branch','main')}/{cfg['filePath']}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "ManageSC-CLI"})
        with urllib.request.urlopen(req, timeout=20) as r:
            raw = r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return {"success": False, "message": "File tidak ditemukan di GitHub."}
        if e.code == 403:
            return {"success": False, "message": "Akses ditolak. Pastikan repo public."}
        return {"success": False, "message": f"Gagal mengambil data ({e.code})."}
    except Exception as e:
        return {"success": False, "message": f"Error koneksi: {e}"}

    imported = []
    for line in raw.split("\n"):
        if not line.strip().startswith("### "):
            continue
        parts = line.replace("### ", "").strip().split()
        if len(parts) >= 3:
            username, expiry, ip = parts[0], parts[1], " ".join(parts[2:])
            imported.append({
                "id": str(int(datetime.now().timestamp() * 1000)) + str(len(imported)),
                "username": username,
                "tipeAkun": "unli" if expiry == "lifetime" else "limit",
                "masaAktif": expiry,
                "ipVps": "'" + ip,
                "emailMember": "",
                "ram": "",
                "pesan": "",
            })
    if not imported:
        return {"success": False, "message": "Tidak ada data valid. Format: ### username masaAktif ip"}
    # Pertahankan kredensial akses remote (SSH) yang tersimpan LOKAL saja.
    # GitHub hanya sync username|expiry|ip, sehingga creds tidak pernah di-upload.
    local = load_vps()
    local_by_key = {f"{it.get('username','')}|{it.get('ipVps','').lstrip(chr(39))}": it for it in local}
    merged = []
    for pulled in imported:
        key = f"{pulled['username']}|{pulled['ipVps'].lstrip(chr(39))}"
        loc = local_by_key.get(key)
        if loc and not pulled.get("userSsh") and not pulled.get("passSsh"):
            pulled["userSsh"] = loc.get("userSsh", "")
            pulled["passSsh"] = loc.get("passSsh", "")
            pulled["serverAktif"] = loc.get("serverAktif", True)
        merged.append(pulled)
    save_vps(merged)
    return {"success": True, "message": f"Berhasil import {len(merged)} data (kredensial SSH lokal dipertahankan).", "imported": len(merged)}


def github_sync():
    cfg = load_github()
    if not cfg or not cfg.get("enabled") or not cfg.get("token") or not cfg.get("username") or not cfg.get("repo") or not cfg.get("filePath"):
        return {"success": False, "message": "GitHub belum dikonfigurasi atau auto-sync dinonaktifkan."}
    content = github_content(load_vps())
    url = f"https://api.github.com/repos/{cfg['username']}/{cfg['repo']}/contents/{cfg['filePath']}"
    headers = {"Authorization": f"token {cfg['token']}", "User-Agent": "ManageSC-CLI"}
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=20) as r:
            sha = json.load(r).get("sha")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            sha = None
        elif e.code == 403:
            return {"success": False, "message": "GitHub API rate limit. Coba lagi nanti."}
        else:
            return {"success": False, "message": f"Gagal cek file GitHub: {e}"}
    except Exception as e:
        return {"success": False, "message": f"Error: {e}"}

    import base64
    payload = {"message": "Update VPS list from ManageSC CLI", "content": base64.b64encode(content.encode()).decode()}
    if sha:
        payload["sha"] = sha
    req = urllib.request.Request(url, data=json.dumps(payload).encode(), headers={**headers, "Content-Type": "application/json"}, method="PUT")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            if r.status in (200, 201):
                return {"success": True, "message": "Berhasil sync data ke GitHub!"}
    except urllib.error.HTTPError as e:
        return {"success": False, "message": f"Gagal sync ke GitHub: {e}"}
    except Exception as e:
        return {"success": False, "message": f"Error: {e}"}
    return {"success": False, "message": "Gagal sync (respon tidak dikenali)."}


# ---------------------------------------------------------------------------
# Cloudflare config + DNS helpers
# ---------------------------------------------------------------------------
CF_FILE = os.path.join(DATA_DIR, "cloudflare.json")


def load_cf():
    try:
        with open(CF_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def save_cf(cfg):
    _ensure_dir()
    with open(CF_FILE, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)


def cf_request(method, path, body=None):
    cfg = load_cf()
    if not cfg or not cfg.get("email") or not cfg.get("key"):
        return None, "Cloudflare belum dikonfigurasi. Jalankan `msc dns config`."
    import urllib.request
    url = "https://api.cloudflare.com/client/v4/" + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("X-Auth-Email", cfg["email"])
    req.add_header("X-Auth-Key", cfg["key"])
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.load(r), None
    except urllib.error.HTTPError as e:
        try:
            err = json.load(e).get("errors", [{}])[0].get("message", e.reason)
        except Exception:
            err = e.reason
        return None, f"HTTP {e.code}: {err}"
    except Exception as e:
        return None, f"Error: {e}"


# ---------------------------------------------------------------------------
# SSH helpers (via system ssh / sshpass)
# ---------------------------------------------------------------------------
def ssh_run(host, user, passwd, command, port=22, timeout=60):
    import shutil
    import subprocess
    if passwd:
        if shutil.which("sshpass"):
            cmd = ["sshpass", "-p", passwd, "ssh", "-o", "StrictHostKeyChecking=no",
                   "-o", "ConnectTimeout=15", "-p", str(port), f"{user}@{host}", command]
        else:
            return "Error: sshpass tidak tersedia. Install `sshpass` atau pakai SSH key (kosongkan --ssh-pass)."
    else:
        cmd = ["ssh", "-o", "StrictHostKeyChecking=no", "-o", "ConnectTimeout=15",
               "-p", str(port), f"{user}@{host}", command]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        out = (r.stdout + r.stderr).strip()
        return out[:2000] if out else "(tanpa output)"
    except subprocess.TimeoutExpired:
        return "Error: koneksi timeout"
    except Exception as e:
        return f"Error: {e}"


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------
def print_table(data):
    if not data:
        print("(kosong)")
        return
    headers = ["ID", "Username", "Tipe", "Masa Aktif", "IP VPS", "Email", "RAM", "SSH"]
    rows = []
    for it in data:
        rows.append([
            it.get("id", "")[:8],
            it.get("username", ""),
            it.get("tipeAkun", ""),
            it.get("masaAktif", ""),
            it.get("ipVps", ""),
            it.get("emailMember", ""),
            it.get("ram", ""),
            "on" if it.get("userSsh") else "-",
        ])
    widths = [max(len(h), *(len(r[i]) for r in rows)) for i, h in enumerate(headers)]
    line = " | ".join(h.ljust(widths[i]) for i, h in enumerate(headers))
    print(line)
    print("-+-".join("-" * w for w in widths))
    for r in rows:
        print(" | ".join(r[i].ljust(widths[i]) for i in range(len(headers))))


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def cmd_add(args):
    item = add_entry(
        args.username, args.tipe, args.masa_aktif or "", args.ip, args.email, args.ram, args.pesan,
        ssh_user=args.ssh_user, ssh_pass=args.ssh_pass,
        server_aktif=args.server_aktif,
    )
    print("Data ditambahkan:")
    print_table([item])


def cmd_ssh(args):
    vid = resolve_id(args.id)
    if not vid:
        print("ID tidak ditemukan.")
        sys.exit(1)
    vps = next((it for it in load_vps() if it["id"] == vid), None)
    if not vps.get("userSsh") or not vps.get("passSsh"):
        print("User/Password SSH kosong — isi lewat `msc update <id> --ssh-user ... --ssh-pass ...`")
        sys.exit(1)
    host = vps["ipVps"][1:] if vps["ipVps"].startswith("'") else vps["ipVps"]
    if args.setup:
        print("Menjalankan setup otomatis (WendyVpn)...")
        print(ssh_run(host, vps["userSsh"], vps["passSsh"], SETUP_SCRIPT, timeout=120))
    elif args.cmd:
        print(ssh_run(host, vps["userSsh"], vps["passSsh"], args.cmd))
    else:
        print(ssh_run(host, vps["userSsh"], vps["passSsh"], "echo OK-CONNECTED"))


def cmd_dns(args):
    if args.action == "config":
        cfg = load_cf() or {}
        cfg["email"] = args.email or cfg.get("email", "")
        cfg["key"] = args.key or cfg.get("key", "")
        save_cf(cfg)
        print("Konfigurasi Cloudflare disimpan.")
    elif args.action == "zones":
        body, err = cf_request("GET", "zones")
        if err:
            print(err)
            return
        if not body.get("success"):
            print("Gagal:", body.get("errors"))
            return
        for z in body["result"]:
            print(f"{z['id']}  {z['name']}  ({z['status']})")
    elif args.action == "records":
        if not args.zone:
            print("Butuh --zone <zone_id>")
            sys.exit(1)
        body, err = cf_request("GET", f"zones/{args.zone}/dns_records")
        if err:
            print(err)
            return
        if not body.get("success"):
            print("Gagal:", body.get("errors"))
            return
        for r in body["result"]:
            print(f"{r['id']}  {r['type']:6} {r['name']} -> {r['content']}  proxy={'ON' if r.get('proxied') else 'OFF'}")
    elif args.action == "add":
        if not (args.zone and args.type and args.name and args.content):
            print("Butuh --zone --type --name --content")
            sys.exit(1)
        rec = {"type": args.type, "name": args.name, "content": args.content,
               "ttl": int(args.ttl) if args.ttl else 1, "proxied": bool(args.proxied)}
        body, err = cf_request("POST", f"zones/{args.zone}/dns_records", rec)
        if err:
            print(err)
            return
        print("Berhasil" if body.get("success") else f"Gagal: {body.get('errors')}")
    elif args.action == "del":
        if not (args.zone and args.record):
            print("Butuh --zone --record <record_id>")
            sys.exit(1)
        body, err = cf_request("DELETE", f"zones/{args.zone}/dns_records/{args.record}")
        if err:
            print(err)
            return
        print("Berhasil dihapus" if body.get("success") else f"Gagal: {body.get('errors')}")


def cmd_list(args):
    data = load_vps()
    filtered = filter_data(data, search=args.search, start=args.start, end=args.end)
    s = stats(filtered)
    total_pages = max(1, (len(filtered) + args.limit - 1) // args.limit)
    page = max(1, min(args.page, total_pages))
    start_i = (page - 1) * args.limit
    page_data = filtered[start_i:start_i + args.limit]
    print_table(page_data)
    print(f"\nHalaman {page}/{total_pages} | Total {s['totalItems']} | limit {s['activeLimited']} | unli {s['unlimited']}")


def cmd_update(args):
    fields = {
        "username": args.username, "tipeAkun": args.tipe, "masaAktif": args.masa_aktif,
        "ipVps": args.ip, "emailMember": args.email, "ram": args.ram, "pesan": args.pesan,
        "userSsh": args.ssh_user, "passSsh": args.ssh_pass, "serverAktif": args.server_aktif,
    }
    fields = {k: v for k, v in fields.items() if v is not None}
    res = update_entry(args.id, **fields)
    if res is None:
        print("ID tidak ditemukan.")
        sys.exit(1)
    print("Data diperbarui:")
    print_table([res])


def cmd_delete(args):
    ok = delete_entry(args.id)
    print("Berhasil dihapus." if ok else "ID tidak ditemukan.")


def cmd_renew(args):
    res = renew_entry(args.id, args.days)
    if res is None:
        print("ID tidak ditemukan.")
        sys.exit(1)
    print(f"Diperpanjang {args.days} hari -> masa aktif {res['masaAktif']}")


def cmd_check(args):
    res = check_expiry(args.days)
    print(f"=== Kadaluarsa ({len(res['expired'])}) ===")
    print_table(res["expired"])
    print(f"\n=== Peringatan <= {args.days} hari ({len(res['warning'])}) ===")
    print_table([it for it, _ in res["warning"]])


def cmd_github(args):
    if args.action == "config":
        cfg = load_github() or {}
        cfg["username"] = args.username or cfg.get("username", "")
        cfg["repo"] = args.repo or cfg.get("repo", "")
        cfg["branch"] = args.branch or cfg.get("branch", "main")
        cfg["filePath"] = args.file_path or cfg.get("filePath", "")
        cfg["token"] = args.token or cfg.get("token", "")
        cfg["enabled"] = args.enabled if args.enabled is not None else cfg.get("enabled", False)
        save_github(cfg)
        print("Konfigurasi GitHub disimpan.")
    elif args.action == "import":
        r = github_import()
        print(r["message"])
    elif args.action == "sync":
        r = github_sync()
        print(r["message"])
    elif args.action == "show":
        print(github_content(load_vps()))


def build_parser():
    p = argparse.ArgumentParser(prog="msc", description="ManageSC CLI - manajemen penyewaan VPS")
    sub = p.add_subparsers(dest="cmd", required=True)

    a = sub.add_parser("add", help="tambah data VPS")
    a.add_argument("--username", required=True)
    a.add_argument("--tipe", required=True, choices=["limit", "unli"])
    a.add_argument("--masa-aktif", help="hari (untuk limit) atau tanggal YYYY-MM-DD")
    a.add_argument("--ip", required=True)
    a.add_argument("--email", default="")
    a.add_argument("--ram", default="")
    a.add_argument("--pesan", default="")
    a.add_argument("--ssh-user", default="", help="user SSH VPS")
    a.add_argument("--ssh-pass", default="", help="password SSH VPS")
    a.add_argument("--server-aktif", default=True, help="true/false")
    a.set_defaults(func=cmd_add)

    l = sub.add_parser("list", help="tampilkan data (filter & pagination)")
    l.add_argument("--search", help="cari username/ip")
    l.add_argument("--start", help="tanggal awal YYYY-MM-DD")
    l.add_argument("--end", help="tanggal akhir YYYY-MM-DD")
    l.add_argument("--page", type=int, default=1)
    l.add_argument("--limit", type=int, default=10)
    l.set_defaults(func=cmd_list)

    u = sub.add_parser("update", help="update data")
    u.add_argument("id")
    u.add_argument("--username")
    u.add_argument("--tipe", choices=["limit", "unli"])
    u.add_argument("--masa-aktif")
    u.add_argument("--ip")
    u.add_argument("--email")
    u.add_argument("--ram")
    u.add_argument("--pesan")
    u.add_argument("--ssh-user")
    u.add_argument("--ssh-pass")
    u.add_argument("--server-aktif")
    u.set_defaults(func=cmd_update)

    d = sub.add_parser("delete", help="hapus data")
    d.add_argument("id")
    d.set_defaults(func=cmd_delete)

    r = sub.add_parser("renew", help="perpanjang masa aktif")
    r.add_argument("id")
    r.add_argument("--days", type=int, required=True)
    r.set_defaults(func=cmd_renew)

    c = sub.add_parser("check", help="cek kadaluarsa / peringatan")
    c.add_argument("--days", type=int, default=7)
    c.set_defaults(func=cmd_check)

    pi = None  # PIN dihapus

    s = sub.add_parser("ssh", help="remote SSH: test / jalankan perintah / setup otomatis")
    s.add_argument("id", help="id, awalan id, atau username VPS")
    s.add_argument("--cmd", help="perintah yang dijalankan di VPS")
    s.add_argument("--setup", action="store_true", help="jalankan skrip setup otomatis (WendyVpn)")
    s.set_defaults(func=cmd_ssh)

    dns = sub.add_parser("dns", help="kelola DNS Cloudflare")
    dns.add_argument("action", choices=["config", "zones", "records", "add", "del"])
    dns.add_argument("--email")
    dns.add_argument("--key")
    dns.add_argument("--zone", help="zone id")
    dns.add_argument("--record", help="record id (untuk del)")
    dns.add_argument("--type")
    dns.add_argument("--name")
    dns.add_argument("--content")
    dns.add_argument("--ttl", default="1")
    dns.add_argument("--proxied", action="store_true")
    dns.set_defaults(func=cmd_dns)

    g = sub.add_parser("github", help="integrasi GitHub")
    g.add_argument("action", choices=["config", "import", "sync", "show"])
    g.add_argument("--username")
    g.add_argument("--repo")
    g.add_argument("--branch")
    g.add_argument("--file-path")
    g.add_argument("--token")
    g.add_argument("--enabled", type=lambda x: x.lower() == "true", choices=[True, False])
    g.set_defaults(func=cmd_github)

    return p


def main():
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
