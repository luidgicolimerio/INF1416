# Cofre Digital

## Requisitos

- JDK 21.
- Celular com o Google Authenticator (data e hora automáticas).
- As bibliotecas já estão em `lib/`: BouncyCastle (`bcprov-jdk18on-1.86.jar`, para o bcrypt) e o driver JDBC do SQLite (`sqlite-jdbc-3.53.4.0.jar`).

## Como rodar

Todos os comandos abaixo partem da raiz do projeto (`CofreSeguro/`). O banco `cofre.db` é criado na pasta de onde o programa é executado, e os caminhos do `Pacote-T3` usados nos exemplos são relativos a ela.

```bash
javac -d bin -cp "lib/*" $(find src -name "*.java" -not -path "src/testes/*")
mkdir -p bin/cofre/model/dados && cp src/cofre/model/dados/*.sql bin/cofre/model/dados/
java -cp "bin:lib/*" cofre.App
```

O segundo comando copia os scripts do banco (`schema.sql` e `mensagens.sql`) para junto das classes. Sem eles, o programa não consegue criar as tabelas na primeira execução.

No Windows, o separador do `-cp` é `;` em vez de `:`:

```bat
java -cp "bin;lib/*" cofre.App
```

No Eclipse ou no VSCode, basta abrir o projeto e executar a classe `cofre.App`. As bibliotecas já estão no `.classpath` e os `.sql` são copiados automaticamente.

### Primeira execução

Com o banco vazio, o programa abre direto o cadastro do administrador. Com os arquivos do `Pacote-T3`:

- Certificado: `src/Pacote-T3/Keys/admin-x509.crt`
- Chave privada: `src/Pacote-T3/Keys/admin-pkcs8-aes.key`
- Frase secreta: `admin`
- Senha pessoal: 8 a 10 dígitos, sem o mesmo dígito duas vezes seguidas e sem ser uma sequência como `12345678` (ex.: `13572468`)

Depois de confirmar os dados do certificado, o programa mostra a chave TOTP em BASE32. Ela aparece só nesse momento, então cadastre-a no Google Authenticator antes de clicar em Concluir (+ → Inserir chave de configuração → tipo Baseada em tempo).

Os usuários `user01` e `user02` do `Pacote-T3` são cadastrados pelo administrador, pela opção 1 do menu, com as frases `user01` e `user02`.

### Execuções seguintes

O programa pede a frase secreta do administrador (`admin`) antes do login. Se a frase estiver errada, o sistema é encerrado.

O login tem três etapas: login name (e-mail do certificado), senha pelo teclado virtual e o código de 6 dígitos do Google Authenticator. O código depende da hora: se for recusado mesmo estando certo, confira se o relógio do computador e o do celular estão com no máximo 30 segundos de diferença.

### Consulta da pasta segura

Na opção 2 do menu, use o caminho `src/Pacote-T3/Files` e a frase secreta do usuário logado. Ao clicar num arquivo da lista, ele é decriptado e gravado com o nome original na mesma pasta. Os arquivos decriptados são apagados quando o programa é encerrado (botão Encerrar Sistema ou fechando a janela).

### Para começar do zero

```bash
rm cofre.db
```

## logView

Programa separado, só para o administrador, que mostra os registros em ordem cronológica. Precisa ser executado num terminal (a frase secreta é lida sem eco, o que não funciona no console do Eclipse) e na mesma pasta do `cofre.db`:

```bash
java -cp "bin:lib/*" logview.LogView src/Pacote-T3/Keys/admin-pkcs8-aes.key
```

- O argumento é o caminho da chave privada do administrador.
- A frase secreta (`admin`) é pedida em seguida.
- Se a chave ou a frase não forem as do administrador, o programa encerra sem mostrar os registros.

## Testes

Os testes usam JUnit 5. No Eclipse ou no VSCode, execute a pasta `src/testes`. Pelo terminal, com o JUnit Console Standalone (`junit-platform-console-standalone-1.14.4.jar`, do Maven Central) em `lib/test/`:

```bash
javac -d bin -cp "lib/*:lib/test/*" $(find src -name "*.java")
mkdir -p bin/cofre/model/dados && cp src/cofre/model/dados/*.sql bin/cofre/model/dados/
java -jar lib/test/junit-platform-console-standalone-1.14.4.jar execute \
  -cp "bin:lib/bcprov-jdk18on-1.86.jar:lib/sqlite-jdbc-3.53.4.0.jar" --select-package testes
```

Os testes rodam a partir da raiz do projeto, porque usam os arquivos de `src/Pacote-T3/`.
