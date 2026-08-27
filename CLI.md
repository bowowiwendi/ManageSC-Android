# ManageSC CLI

Alat baris perintah (CLI) untuk manajemen penyewaan VPS, diadaptasi dari
[bowowiwendi/ManageSC-Android](https://github.com/bowowiwendi/ManageSC-Android)
(Android, Kotlin). Berjalan mandiri dengan Python stdlib (tanpa `pip install`).
Data disimpan di `data/vps.json`.

Fitur remote mengikuti aplikasi asli:
- **SSH** (`ssh`) — test koneksi, jalankan perintah, & setup otomatis VPS
- **Cloudflare DNS** (`dns`) — list zona, list/create/delete record
- **GitHub sync** (`github`) — pull/push daftar `### username expiry ip`

## Instalasi
Butuh Python 3.8+. Untuk fitur SSH berpassword, install `sshpass`
(`apt install sshpass` / `brew install hudochenkov/sshpass/sshpass`).
SSH dengan key tidak butuh `sshpass`.

```bash
chmod +x msc.py
```

## Penggunaan

### CRUD lokal
```bash
./msc.py add --username budi --tipe limit --masa-aktif 30 --ip 1.2.3.4 \
             --ssh-user root --ssh-pass rahasia --ram 2GB
./msc.py add --username sari --tipe unli --ip 5.6.7.8 --email sari@x.com
./msc.py list --search budi
./msc.py list --start 2026-01-01 --end 2026-12-31 --page 2 --limit 20
./msc.py update budi --ram 4GB --server-aktif false
./msc.py delete sari
./msc.py renew budi --days 10
./msc.py check --days 7
```

### Remote SSH (`ssh/RemoteSsh.kt`)
```bash
./msc.py ssh budi                 # test koneksi (echo OK-CONNECTED)
./msc.py ssh budi --cmd "uptime"  # jalankan perintah
./msc.py ssh budi --setup         # jalankan skrip auto-setup WendyVpn
```
Id bisa berupa id penuh, awalan id, atau username. Butuh `--ssh-user` &
`--ssh-pass` (diisi saat `add`/`update`).

### Cloudflare DNS (`cloudflare/CloudflareHelper.kt`)
```bash
./msc.py dns config --email anda@email.com --key <global_api_key>
./msc.py dns zones
./msc.py dns records --zone <zone_id>
./msc.py dns add --zone <zone_id> --type A --name sub.domain.com --content 1.2.3.4 --proxied
./msc.py dns del --zone <zone_id> --record <record_id>
```

### GitHub (`github/GitHubSync.kt`)
```bash
./msc.py github config --username bowowiwendi --repo ipvps --file-path main/ip --token TOKEN --enabled true
./msc.py github show      # generate ### user expiry ip
./msc.py github import    # tarik (pull) — kredensial SSH lokal dipertahankan
./msc.py github sync      # push (replace isi file)
```

## Struktur Data
Sama dengan `VpsDbHelper` di Android:
`id, username, tipeAkun, masaAktif, ipVps, emailMember, ram, pesan,
userSsh, passSsh, serverAktif`

> PIN (di aplikasi asli via `Prefs.kt`) **tidak** disertakan di CLI ini.
