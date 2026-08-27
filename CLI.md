# ManageSC CLI

Alat manajemen penyewaan VPS berbasis terminal, diadaptasi dari
[bowowiwendi/ManageSC-Android](https://github.com/bowowiwendi/ManageSC-Android)
(Android, Kotlin). Python stdlib saja (tanpa `pip install`). Data di `data/*.json`.

## Catatan nama perintah
`msc` adalah nama compiler C# **Mono** di Termux/Alpine, sehingga command tool ini
adalah **`managesc`** agar tidak bentrok. (Jika Anda yakin tidak pakai Mono, bebas
ganti namanya saat install.)

## Instal di Termux
```sh
pkg install -y python3
wget -qO $PREFIX/bin/managesc https://raw.githubusercontent.com/bowowiwendi/ManageSC-Android/main/msc \
  && chmod +x $PREFIX/bin/managesc
managesc
```
(Python3 + curses biasanya sudah ada di Termux; tidak perlu `ncurses` terpisah.)

## Instal di Alpine
```sh
apk add --no-cache python3 ncurses
wget -qO /usr/local/bin/managesc https://raw.githubusercontent.com/bowowiwendi/ManageSC-Android/main/msc \
  && chmod +x /usr/local/bin/managesc
managesc
```

## Jalankan
```sh
managesc            # TUI curses (terminal asli) ATAU fallback menu teks (non-TTY)
managesc --help     # mode perintah (argparse)
```
Jika dijalankan dari runner non-TTY (mis. opencode), otomatis fallback ke menu teks
bernomor — semua fitur tetap bisa.

## Menu (TUI / teks)
Lihat Daftar, Tambah, Cari, Update, Hapus, Perpanjang, Cek Kadaluarsa,
SSH Remote, DNS Cloudflare, GitHub.

## Mode perintah (argparse)
```bash
managesc add --username budi --tipe limit --masa-aktif 30 --ip 1.2.3.4 \
        --ssh-user root --ssh-pass rahasia --ram 2GB
managesc list --search budi
managesc update budi --ram 4GB --server-aktif false
managesc delete budi
managesc renew budi --days 10
managesc check --days 7
```

### Remote SSH
```bash
managesc ssh budi                 # test koneksi
managesc ssh budi --cmd "uptime"  # jalankan perintah
managesc ssh budi --setup         # setup otomatis WendyVpn
```

### Cloudflare DNS
```bash
managesc dns config --email a@b.com --key <global_api_key>
managesc dns zones
managesc dns records --zone <zone_id>
managesc dns add --zone <zone_id> --type A --name sub.domain.com --content 1.2.3.4 --proxied
managesc dns del --zone <zone_id> --record <record_id>
```

### GitHub
```bash
managesc github config --username bowowiwendi --repo ipvps --file-path main/ip --token TOKEN --enabled true
managesc github show      # generate ### user expiry ip
managesc github import    # pull (kredensial SSH lokal dipertahankan)
managesc github sync      # push
```

## Keamanan
Kredensial akses remote (SSH `userSsh`/`passSsh`, Cloudflare key, GitHub token)
hanya disimpan di file lokal `data/*.json` dan **tidak pernah di-upload** ke GitHub
(push hanya mengirim `### username expiry ip`).

## Struktur Data
`id, username, tipeAkun, masaAktif, ipVps, emailMember, ram, pesan,
userSsh, passSsh, serverAktif`
