# DigestCalculator

## Como rodar

Todos os comandos abaixo partem da raiz do projeto.

```bash
javac -d bin src/DigestCalculator.java
cp ArqListaDigestBackup.xml ArqListaDigest.xml # Restaura a lista original
java -cp bin DigestCalculator <Tipo_Digest> <Caminho_ArqListaDigest> <Caminho_da_Pasta_dos_Arquivos>
```

- `Tipo_Digest`: `MD5`, `SHA1`, `SHA256` ou `SHA512`.
- `Caminho_ArqListaDigest`: arquivo XML com os digests conhecidos. Se ele não existir ou estiver vazio, é tratado como uma lista sem entradas e é criado ao final da execução.
- `Caminho_da_Pasta_dos_Arquivos`: pasta com os arquivos a serem processados.

Se algum argumento estiver faltando, ou se o tipo de digest não for um dos quatro aceitos, o programa imprime a orientação de uso e encerra.

### Exemplo com os casos de teste

```bash
javac -d bin src/DigestCalculator.java
cp ArqListaDigestBackup.xml ArqListaDigest.xml
java -cp bin DigestCalculator MD5 ArqListaDigest.xml tests
```

Saída:

```
acentos.txt MD5 bf92ac216e3b7cccd6e4626fba87f0fc (OK)
binario.bin MD5 ba1c90179244f0c93592b93985781f7e (OK)
exato.dat MD5 8fda0416eb9b58adcdbdcb0f9d1afdc4 (NOT OK)
grande.dat MD5 b06f302ed48de02424ce96cb2a53f745 (NOT FOUND)
quebrado.dat MD5 f36238b775e51e46235d71dfe658ece6 (OK)
texto_simples.txt MD5 3d5f58424a05eb6150d7eeff5b3ff781 (OK)
vazio.dat MD5 d41d8cd98f00b204e9800998ecf8427e (COLISION)
zero_esquerda_md5.txt MD5 0a7f382594cd4ccf01065dfe4c2026b4 (OK)
zero_esquerda_sha1.txt MD5 9922f8a5c861eb5ccb0696c786f684c2 (NOT FOUND)
zero_esquerda_sha256.txt MD5 f9e0b213750c7e5f45aaef8f7a331f86 (NOT FOUND)
zero_esquerda_sha512.txt MD5 73deb27a6a363700cbae3986600ea0e6 (NOT FOUND)
```

> **Importante:** o programa altera o `ArqListaDigest.xml`, porque acrescenta os digests NOT FOUND. Depois da primeira execução, esses arquivos passam a dar OK. Para repetir o teste do zero, restaure a lista a partir do `ArqListaDigestBackup.xml`.

## Fluxo do programa

### 1. Validação dos argumentos

O programa exige 3 argumentos. O tipo é convertido para o nome padrão da JCA (`SHA1` → `SHA-1`, `SHA256` → `SHA-256`, `SHA512` → `SHA-512`). Na saída e no XML continua sendo usado o nome sem hífen, como no enunciado.

### 2. Leitura do XML (`lerCatalogo`)

O XML é lido com `DocumentBuilder` e guardado num `Map<nome, Map<tipo, hex>>`

### 3. Cálculo dos digests (`calcularDigestsDaPasta`)

Para cada arquivo da pasta (subpastas são ignoradas), o conteúdo é lido em blocos de 8 KB. Cada bloco é passado ao `MessageDigest.update(byte[], int, int)`, então arquivos grandes não precisam caber inteiros na memória. O digest é convertido para hexadecimal com 2 dígitos por byte, preservando os zeros à esquerda.

### 4. Classificação (`verificarStatus`)

Só entra na comparação o digest do tipo pedido na linha de comando. Os status são testados nesta ordem:

| Status | Condição |
|---|---|
| `COLISION` | O digest calculado é igual ao de outro arquivo da pasta ou ao de um arquivo com outro nome no XML. Tem precedência sobre os demais |
| `NOT FOUND` | O arquivo não está no XML, ou está mas não tem digest do tipo pedido |
| `OK` | O digest do tipo pedido no XML é igual ao calculado |
| `NOT OK` | O digest do tipo pedido no XML é diferente do calculado |

Para detectar colisões, o programa monta um mapa `hash → nomes de arquivo` com os hashes da pasta e os do XML, só do tipo pedido. Há colisão quando um hash está associado a mais de um nome. Quando o mesmo arquivo aparece na pasta e no XML, isso não conta como colisão.

### 5. Saída (`imprimirResultados`)

Uma linha por arquivo da pasta, em ordem alfabética:

```
Nome_Arq Tipo_Digest Digest_Hex (STATUS)
```

### 6. Atualização do XML (`atualizarCatalogo`)

Para cada arquivo com status `NOT FOUND`:

- se já existe um `<FILE_ENTRY>` com esse nome, um novo `<DIGEST_ENTRY>` é acrescentado dentro dele;
- se não existe, um novo `<FILE_ENTRY>` é criado no final do `<CATALOG>`.

Os arquivos com `COLISION`, `OK` ou `NOT OK` não são gravados. As entradas que já existiam são mantidas sem alteração, inclusive as de arquivos que não estão na pasta. O arquivo é regravado com `Transformer`, indentado com 4 espaços e sem a declaração `<?xml ...?>`, e só quando há algo novo para acrescentar.