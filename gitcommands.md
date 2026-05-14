# BFG Repo Cleaner ile (önerilen — daha hızlı)
brew install bfg   # macOS
bfg --replace-text passwords.txt marine-management-system.git

# Veya git filter-repo ile
git filter-repo --replace-text <(echo 'maritime2025!==>***REMOVED***
SuperAdmin123!==>***REMOVED***
marineManagementSuperSecretKey2025ForCaptainTahir==>***REMOVED***
rmrxlqorghzvnrmp==>***REMOVED***')



Şimdi terminalinizde şu komutları sırayla çalıştırın:
1. Bare clone oluşturun (repo klasörünüzün bir üst dizininde):
   bashcd ~
   git clone --mirror ~/Documents/marine-management-system marine-management-system.git
2. BFG'yi çalıştırın:
   bashbfg --replace-text ~/Documents/marine-management-system/passwords.txt marine-management-system.git
3. Git geçmişini temizleyin:
   bashcd marine-management-system.git
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
4. Temizlenmiş geçmişi push edin:
   bashgit push --force
5. passwords.txt'yi silin (güvenlik için):
   bashrm ~/Documents/marine-management-system/passwords.txt

Bir de şunu bilmenizi isterim: bu şifreler zaten GitHub'a push edilmiş durumdaysa, şifreleri hemen değiştirmeniz gerekiyor — veritabanı şifresi, JWT secret ve Supabase/Postgres anahtarı. Geçmişteki commit'ler git temizliği öncesinde görüntülenmiş olabilir.You've used 75% of your weekly limitGet more usageSonnet 4.6Claude is AI and can make mistakes. Please double-check responses.Progresspasswords.txt dosyasını oluştur2Bare clone alınıyor3BFG ile şifreleri geçmişten sil4Temizlenmiş repo'yu push etWorking foldersmarine-management-systemInstructions · CLAUDE.mdpasswords.txtmarine-managment-angularScratchpadContextTrack tools and referenced files used in this task.