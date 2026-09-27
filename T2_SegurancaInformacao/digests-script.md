Rodar dentro da pasta tests/

```
for t in md5 sha1 sha256 sha512; do
  for f in *; do
    echo "$f $(echo $t | tr a-z A-Z) $(${t}sum "$f" | cut -d' ' -f1)"
  done
done > ../digests.txt
```

Se for fazer um por um (no Linux):

```
md5sum    arquivo
sha1sum   arquivo
sha256sum arquivo
sha512sum arquivo
```