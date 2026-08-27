# ManageSC CLI

Alat manajemen penyewaan VPS berbasis terminal, diadaptasi dari
[bowowiwendi/ManageSC-Android](https://github.com/bowowiwendi/ManageSC-Android)
(Android, Kotlin). Python stdlib saja (tanpa `pip install`). Data di `data/*.json`.

## Jalankan cukup dengan `msc`
File `msc` adalah script Python ber-shebang. Setelah ada di PATH:
```sh
msc            # langsung masuk TUI (menu interaktif)
msc --help     # mode perintah (argparse)
```

## Instal di Alpine (satu command)
```sh
apk add --no-cache python3 ncurses && \
wget -qO /usr/local/bin/msc https://raw.githubusercontent.com/bowowiwendi/ManageSC-Android/main/msc && \
chmod +x /usr/local/bin/msc && msc
```
(`ncurses` agar modul `curses` tersedia untuk TUI.)

## TUI (menu interaktif)
Jalankan `msc` tanpa argumen. Navigasi:
- `↑` / `↓` pilih menu, `Enter` jalankan, `q` / `Esc` kembali.
- Menu: Lihat Daftar, Tambah, Cari, Update, Hapus, Perpanjang,
  Cek Kadaluarsa, SSH Remote, DNS Cloudflare, GitHub.
- Form input ditampilkan baris bawah; kosongkan field untuk skip (saat Update).

## Mode perintah (argparse)
```bash
msc add --username budi --tipe limit --masa-aktif 30 --ip 1.2.3.4 \
        --ssh-user root --ssh-pass rahasia --ram 2GB
msc list --search budi
msc update budi --ram 4GB --server-aktif false
msc delete budi
msc renew budi --days 10
msc check --days 7
```

### Remote SSH
```bash
msc ssh budi                 # test koneksi
msc ssh budi --cmd "uptime"  # jalankan perintah
msc ssh budi --setup         # setup otomatis WendyVpn
```

### Cloudflare DNS
```bash
msc dns config --email a@b.com --key <global_api_key>
msc dns zones
msc dns records --zone <zone_id>
msc dns add --zone <zone_id> --type A --name sub.domain.com --content 1.2.3.4 --proxied
msc dns del --zone <zone_id> --record <record_id>
```

### GitHub
```bash
msc github config --username bowowiwendi --repo ipvps --file-path main/ip --token TOKEN --enabled true
msc github show      # generate ### user expiry ip
msc github import    # pull (kredensial SSH lokal dipertahankan)
msc github sync      # push
```

## Keamanan
Kredensial akses remote (SSH `userSsh`/`passSsh`, Cloudflare key, GitHub token)
hanya disimpan di file lokal `data/*.json` dan **tidak pernah di-upload** ke GitHub
(push hanya mengirim `### username expiry ip`).

## Struktur Data
`id, username, tipeAkun, masaAktif, ipVps, emailMember, ram, pesan,
userSsh, passSsh, serverAktif`
