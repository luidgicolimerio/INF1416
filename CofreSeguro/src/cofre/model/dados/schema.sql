-- Estrutura do banco do Cofre Digital (SQLite).
-- Executado pelo BancoDeDados quando o arquivo do banco ainda não existe.
-- Não usar ponto e vírgula em comentários: o BancoDeDados separa os comandos por ele.
-- Atenção: o SQLite só respeita as chaves estrangeiras com "PRAGMA foreign_keys = ON" em cada conexão.

CREATE TABLE Grupos (
    gid   INTEGER PRIMARY KEY,
    nome  TEXT NOT NULL UNIQUE
);

INSERT INTO Grupos (gid, nome) VALUES (1, 'Administrador');
INSERT INTO Grupos (gid, nome) VALUES (2, 'Usuário');

-- Usuarios e Chaveiro apontam um para o outro: no cadastro, insere o usuário com kid NULL,
-- insere o par no Chaveiro com o uid e depois atualiza o kid do usuário (tudo numa transação).
CREATE TABLE Usuarios (
    uid              INTEGER PRIMARY KEY AUTOINCREMENT,
    login_name       TEXT    NOT NULL UNIQUE,              -- e-mail do Sujeito do certificado
    nome             TEXT    NOT NULL,                     -- CN do Sujeito do certificado
    gid              INTEGER NOT NULL REFERENCES Grupos(gid),
    kid              INTEGER REFERENCES Chaveiro(kid),
    senha_hash       TEXT    NOT NULL,                     -- bcrypt $2y$08$... (60 caracteres)
    chave_totp       BLOB    NOT NULL,                     -- chave TOTP em BASE32 cifrada com AES/ECB/PKCS5Padding
    bloqueado_ate    INTEGER,                              -- instante (ms desde 1970) do fim do bloqueio, NULL se livre
    total_acessos    INTEGER NOT NULL DEFAULT 0,
    total_consultas  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE Chaveiro (
    kid              INTEGER PRIMARY KEY AUTOINCREMENT,
    uid              INTEGER NOT NULL UNIQUE REFERENCES Usuarios(uid),
    certificado_pem  TEXT    NOT NULL,                     -- certificado X.509 em PEM
    chave_privada    BLOB    NOT NULL                      -- arquivo da chave privada, ainda cifrado (binário)
);

CREATE TABLE Mensagens (
    mid    INTEGER PRIMARY KEY,
    texto  TEXT NOT NULL                                   -- com os marcadores <login_name> e <arq_name>
);

-- Não guarda o texto da mensagem: o logView monta o texto a partir de Mensagens, Usuarios e arquivo.
CREATE TABLE Registros (
    rid        INTEGER PRIMARY KEY AUTOINCREMENT,
    mid        INTEGER NOT NULL REFERENCES Mensagens(mid),
    data_hora  TEXT    NOT NULL,                           -- 'yyyy-MM-dd HH:mm:ss.SSS'
    uid        INTEGER REFERENCES Usuarios(uid),           -- NULL quando não há usuário identificado
    arquivo    TEXT                                        -- nome do arquivo selecionado (7015 a 7021)
);
