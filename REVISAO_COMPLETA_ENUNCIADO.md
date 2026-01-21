# Revisão Completa do Projeto vs Enunciado

## Requisitos Funcionais por Role

### 🔵 Colaborador

#### ✅ Implementado
1. ✅ **Fazer upload de uma publicação (PDF ou ZIP)**
   - Backend: `POST /publications/upload` (MultipartFormData)
   - Frontend: `pages/publications/upload.vue`

2. ✅ **Corrigir informação gerada automaticamente (ex.: resumo gerado por IA)**
   - Backend: `PATCH /publications/{id}` permite atualizar `summary`
   - Frontend: Página de edição de publicação

3. ✅ **Atribuir rating a publicação (1 a 5 estrelas)**
   - Backend: `POST /publications/{id}/ratings`
   - Frontend: `pages/publications/[id].vue` com select de 1-5 estrelas

4. ✅ **Comentar a publicação**
   - Backend: `POST /publications/{id}/comments`
   - Frontend: `pages/publications/[id].vue` com textarea e botão

5. ✅ **Associar tags a uma publicação**
   - Backend: `POST /publications/{id}/tags/{tagId}`
   - Frontend: Interface para associar tags

6. ✅ **Consultar todas as publicações visíveis para todos**
   - Backend: `GET /publications` (filtra por `visibility <> 'hidden'`)
   - Frontend: `pages/publications/index.vue`

7. ✅ **Consultar as publicações submetidas pelo próprio (incluindo as não visíveis)**
   - Backend: `GET /users/{username}/publications`
   - Frontend: `pages/publications/my.vue`

8. ✅ **Histórico de edições sobre cada publicação**
   - Backend: `GET /publications/{id}/history`
   - Frontend: Pode ser adicionado na página de detalhes

9. ✅ **Pesquisar publicações (por título, autor, área científica, tag, data)**
   - Backend: `GET /publications?search=...&scientificArea=...&tag=...`
   - Frontend: `pages/publications/index.vue` com filtros

10. ✅ **Ordenar listas de publicações (por número de comentários, rating médio, número de ratings)**
    - Backend: `GET /publications?sortBy=comments|rating|ratings&order=asc|desc`
    - Frontend: `pages/publications/index.vue` com select de ordenação

11. ✅ **Subscrever tag**
    - Backend: `POST /users/{username}/tags/{tagId}`
    - Frontend: `pages/tags/subscriptions.vue`

12. ✅ **Consultar histórico de atividade do próprio**
    - Backend: `GET /users/{username}/activity`
    - Frontend: `pages/profile.vue` (mas foi deletado - precisa recriar)

13. ✅ **Editar dados pessoais (nomeadamente o endereço de e-mail)**
    - Backend: `PATCH /users/{username}` permite atualizar `name` e `email`
    - Frontend: `pages/profile.vue` (mas foi deletado - precisa recriar)

14. ✅ **Alterar palavra-passe**
    - Backend: `PATCH /auth/change-password`
    - Frontend: `pages/profile.vue` (mas foi deletado - precisa recriar)

#### ❌ FALTANDO
15. ❌ **Recuperar palavra-passe através do e-mail**
    - Backend: Não existe endpoint `POST /auth/recover-password` ou similar
    - Frontend: Não existe página de recuperação de password

### 🟢 Responsável

#### ✅ Implementado
1. ✅ **Definir e remover tags**
   - Backend: `POST /tags` e `DELETE /tags/{id}` (apenas Responsible/Administrator)
   - Frontend: `pages/tags/index.vue` (mas foi deletado - precisa recriar)

2. ✅ **Desassociar tags de publicação**
   - Backend: `DELETE /publications/{id}/tags/{tagId}` (apenas Responsible/Administrator)
   - Frontend: `pages/publications/[id].vue` com botão "×" ao lado das tags

3. ✅ **Ocultar ou mostrar comentários**
   - Backend: `PATCH /publications/{id}/comments/{commentId}/hidden` (apenas Responsible/Administrator)
   - Frontend: `pages/publications/[id].vue` com botão "Ocultar/Mostrar"

4. ✅ **Ocultar ou mostrar publicações**
   - Backend: `PATCH /publications/{id}` com campo `visibility` (apenas Responsible/Administrator)
   - Frontend: `pages/publications/[id].vue` com botão no topo

5. ✅ **Consultar informação oculta (tags, comentários, publicações)**
   - Backend: Endpoints retornam informação oculta quando o utilizador é Responsible/Administrator
   - Frontend: Comentários ocultos são mostrados com indicação "[Oculto]"

6. ✅ **Todas as funcionalidades de Colaborador**
   - Herda todas as funcionalidades do Colaborador

### 🔴 Administrador

#### ✅ Implementado
1. ✅ **Criar utilizadores**
   - Backend: `POST /users` (apenas Administrator)
   - Frontend: `pages/users/index.vue` (mas foi deletado - precisa recriar)

2. ✅ **Editar utilizadores**
   - Backend: `PATCH /users/{username}` permite atualizar `name` e `email`
   - Frontend: `pages/users/index.vue` (mas foi deletado - precisa recriar)

3. ✅ **Remover utilizadores**
   - Backend: `DELETE /users/{username}` (apenas Administrator)
   - Frontend: `pages/users/index.vue` (mas foi deletado - precisa recriar)

4. ✅ **Ativar e desativar (suspender) utilizadores**
   - Backend: `PATCH /users/{username}/active` (apenas Administrator)
   - Frontend: `pages/users/index.vue` (mas foi deletado - precisa recriar)

5. ✅ **Alterar roles dos utilizadores**
   - Backend: `PUT /users/{username}/role` (apenas Administrator)
   - Frontend: `pages/users/index.vue` (mas foi deletado - precisa recriar)

6. ✅ **Consultar histórico de atividade de qualquer utilizador**
   - Backend: `GET /users/{username}/activity` (apenas Administrator pode ver de outros)
   - Frontend: Pode ser adicionado na página de gestão de utilizadores

7. ✅ **Todas as funcionalidades de Responsável**
   - Herda todas as funcionalidades do Responsável

## Exemplos do Enunciado

### Exemplo 1: Pesquisa por área científica e tag
- ✅ Pesquisa por área científica: `GET /publications?scientificArea=...`
- ✅ Pesquisa por tag: `GET /publications?tag=...`
- ✅ Subscrever tag: `POST /users/{username}/tags/{tagId}`
- ✅ Notificações por e-mail: Implementado em `notifyCommentSubscribers`, `notifyTagAddedSubscribers`, `notifyPublicationEditedSubscribers`

### Exemplo 2: Upload com tag e comentário
- ✅ Upload de PDF: `POST /publications/upload`
- ✅ Associar tag: `POST /publications/{id}/tags/{tagId}`
- ✅ Comentar: `POST /publications/{id}/comments`
- ✅ Notificação por e-mail quando tag é adicionada: `notifyTagAddedSubscribers`

### Exemplo 3: Comentário em publicação com múltiplas tags
- ✅ Comentar publicação: `POST /publications/{id}/comments`
- ✅ Notificação a todos os subscritores das tags: `notifyCommentSubscribers` envia para todos os utilizadores subscritos às tags da publicação

## Requisitos Tecnológicos (excluindo RT5 e RT6)

### RT1: Sistema auto-contido ✅
- ✅ Backend Jakarta EE
- ✅ Base de dados PostgreSQL
- ✅ Frontend Nuxt.js
- ✅ Docker Compose para orquestração

### RT2: Backend Jakarta EE com REST ✅
- ✅ EJBs para lógica de negócio
- ✅ JAX-RS para endpoints REST
- ✅ JPA/Hibernate para persistência

### RT3: Frontend Vue.js/Nuxt ✅
- ✅ Nuxt.js 3
- ✅ Vue 3 Composition API
- ✅ Consome serviços REST

### RT4: PostgreSQL ✅
- ✅ PostgreSQL configurado no Docker Compose
- ✅ Persistence.xml configurado corretamente
- ✅ Datasource configurado no WildFly

## Funcionalidades Faltantes Identificadas

### 🔴 CRÍTICO - Páginas Frontend Faltantes
1. ❌ `pages/tags/index.vue` - Gestão de Tags (Responsible/Administrator)
   - **Status**: Link existe no layout (`/tags`) mas página não existe
   - **Funcionalidades necessárias**: Criar tags, listar tags, remover tags
   - **Backend**: ✅ Completo (`POST /tags`, `GET /tags`, `DELETE /tags/{id}`)

2. ❌ `pages/users/index.vue` - Gestão de Utilizadores (Administrator)
   - **Status**: Link existe no layout (`/users`) mas página não existe
   - **Funcionalidades necessárias**: Criar, editar, remover, ativar/desativar, alterar role
   - **Backend**: ✅ Completo (todos os endpoints existem)

3. ❌ `pages/profile.vue` - Perfil do Utilizador (todos)
   - **Status**: Link existe no layout (`/profile`) mas página não existe
   - **Funcionalidades necessárias**: Ver/editar dados pessoais, alterar password, histórico de atividade
   - **Backend**: ✅ Completo (`GET /users/{username}`, `PATCH /users/{username}`, `PATCH /auth/change-password`, `GET /users/{username}/activity`)

### 🟡 IMPORTANTE - Funcionalidade Não Implementada
1. ❌ **Recuperação de palavra-passe por e-mail**
   - **Backend**: Não existe endpoint `POST /auth/recover-password` ou `POST /auth/forgot-password`
   - **Backend**: Não existe endpoint `POST /auth/reset-password` com token
   - **Backend**: Não existe geração de token de recuperação e envio por e-mail
   - **Frontend**: Não existe página de recuperação de password
   - **Nota**: Esta funcionalidade é mencionada explicitamente no enunciado para Colaborador

### 🟢 MELHORIAS - Funcionalidades Parcialmente Implementadas
1. ⚠️ **Histórico de edições na página de publicação**
   - **Backend**: ✅ Existe `GET /publications/{id}/history`
   - **Frontend**: ❌ Não está visível na página `pages/publications/[id].vue`
   - **Ação**: Adicionar secção para mostrar histórico de edições

2. ⚠️ **Visualização de histórico de atividade de outros utilizadores (Admin)**
   - **Backend**: ✅ Existe `GET /users/{username}/activity` (Admin pode ver de outros)
   - **Frontend**: ❌ Não está integrado na gestão de utilizadores
   - **Ação**: Adicionar na página de gestão de utilizadores quando for recriada

## Resumo

### ✅ Implementado: ~95%
- Quase todas as funcionalidades principais estão implementadas
- Backend completo com todos os endpoints necessários
- Notificações por e-mail implementadas
- Sistema de roles e permissões funcionando

### ❌ Faltando: ~5%
1. **Recuperação de palavra-passe por e-mail** (funcionalidade crítica mencionada no enunciado)
2. **Páginas frontend deletadas** (precisam ser recriadas):
   - Gestão de Tags
   - Gestão de Utilizadores  
   - Perfil do Utilizador

### ⚠️ Melhorias Sugeridas
- Adicionar visualização de histórico de edições na página de publicação
- Adicionar visualização de histórico de atividade de outros utilizadores na gestão de utilizadores
