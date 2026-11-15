# Два шага в консоли Google

Эти два шага скрипт настройки сделать не может. Их один раз выполняет владелец
проекта, войдя тем же аккаунтом Google, на котором заведён проект. Скрипт
печатает обе ссылки уже с именем проекта. [English version](console-steps.md).

**Перед началом:** с 18 октября 2026 года Google пускает в консоли Firebase и
Google Cloud только аккаунт с включённой двухэтапной проверкой. Сначала включите
её в настройках безопасности аккаунта Google, иначе консоли остановятся на
требующем её баннере.

**1. Включить вход через Google.**
Откройте `https://console.firebase.google.com/project/<project-id>/authentication/providers`.
Нажмите Get started, если такая кнопка есть, затем Google, Enable. В поле
Public-facing name for project впишите название группы — его участники увидят
при входе. В поле адреса поддержки выберите свой адрес. Нажмите Save.

После этого консоль может показать «Can't enable Google sign-in for some apps»
и «An identical OAuth client already exists» рядом с приложением Android. Так и
должно быть, исправлять ничего не нужно: приложение у всех групп — одна сборка с
одним ключом подписи, а такой клиент OAuth может быть только у одного проекта в
мире, поэтому приложение входит через браузер
([ADR-0002](ADR/0002-sign-in-through-the-browser.md)). Закройте окно — вход через
Google остаётся включённым.

**2. Разрешить странице вход со своего адреса.**
Откройте `https://console.cloud.google.com/auth/clients?project=<project-id>`.
Откройте «Web client (auto created by Google Service)». В разделе Authorized
redirect URIs нажмите Add URI и вставьте `https://<site>.web.app/__/auth/handler`.
Нажмите Save.

**Затем** сообщите тому, кто запускает скрипт, что оба шага выполнены.

**При первом входе** на страницу группы или в приложение Google спросит, можно ли
`<site>.web.app` (из приложения — `<project-id>.firebaseapp.com`) узнать ваш
адрес почты и профиль. Это адрес самой группы, а не чужого сервиса; разрешите.
Участники видят тот же экран один раз, при первом входе.

**Не трогать:** тариф Blaze, оплату и Firebase AI Logic. Проект работает на
бесплатном тарифе, а с включённым AI Logic его квоту сможет расходовать кто
угодно.

**Только для устройства без аккаунта Google: вход по паролю.**
Устройство, на котором не должно быть аккаунта Google, входит по адресу и
паролю, которые для него заводит владелец проекта
([ADR-0046](ADR/0046-a-device-signs-in-with-a-password.md)). Если такого
устройства в группе нет, шаг пропустите. Провайдер включается один раз: в
`https://console.firebase.google.com/project/<project-id>/authentication/providers`
нажмите Add new provider, Email/Password, Enable («Email link» не включайте),
Save — или выполните `tools/project.sh enable-email-sign-in <project-id>`.
Затем положите пароль в файл, который можете читать только вы, и заведите
аккаунт:

```sh
install -m 600 /dev/null ~/.config/geoshare/device-password
# впишите пароль в этот файл редактором, одной строкой
tools/project.sh add-account <project-id> --email you+device@gmail.com \
    --password-file ~/.config/geoshare/device-password
```

Адрес с `+что-то` приходит в ваш же почтовый ящик, а для Firebase это
отдельный адрес. Аккаунт заводится сразу подтверждённым, поэтому
администратор может выписать на него приглашение. Чтобы сменить пароль,
впишите новый в тот же файл и выполните `tools/project.sh set-password` с теми
же параметрами; устройство после этого попросит войти заново.
