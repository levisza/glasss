# GlassLaunch Compose — Galaxy A36

Projeto de launcher Android em Kotlin + Jetpack Compose inspirado visualmente no Liquid Glass. Não utiliza nem copia código privado da Apple; implementa efeitos originais com APIs Android.

## Recursos desta base
- Activity registrada como HOME/launcher e também acessível pelo ícone do app.
- Lista aplicativos instalados através de intents `MAIN` + `LAUNCHER` e inicia os apps escolhidos.
- Tela inicial com relógio/data, widgets visuais, pesquisa e dock translúcido.
- Biblioteca com filtro de pesquisa.
- Fundo em gradientes, camadas luminosas, contornos especulares e animação de escala ao toque.
- Dependências de Jetpack Compose e Material 3.

## Abrir e compilar
1. Abra a pasta `GlassLaunch-Compose` no Android Studio atualizado.
2. Use JDK 17 e instale Android SDK Platform 35.
3. Sincronize o Gradle e execute em um aparelho/emulador Android 8+.
4. Para gerar o APK: **Build > Build APK(s)**.
5. No Galaxy A36, selecione GlassLaunch em Configurações > Aplicativos > Escolher apps padrão > App de início (os nomes podem variar por versão One UI).

## Limites conhecidos
- Esta é uma base inicial; precisa de compilação/testes no Android Studio e teste em hardware real antes de ser considerada pronta para distribuição.
- O efeito Liquid Glass do iOS não é uma API disponível no Android. O fundo é desenhado com gradientes/camadas para desempenho; não é uma refração óptica idêntica nem desfoca dinamicamente todo o conteúdo que está atrás.
- Os widgets de clima/calendário são demonstrativos, não ligados a serviços de dados.
- A API do Android limita a visibilidade de alguns apps instalados. Este projeto consulta atividades launcher explicitamente sem solicitar `QUERY_ALL_PACKAGES`.
- Ícones e nomes vêm dos apps instalados no dispositivo; não são cópias dos ícones do iOS.

## Ajustes para o Galaxy A36
O projeto usa Compose, minSdk 26 e targetSdk 35. Os efeitos são leves para manter fluidez; animações e blur adicional podem ser refinados depois de medir FPS e consumo no aparelho.
