# Два шага в консоли Google

Эти два шага скрипт настройки сделать не может. Их один раз выполняет владелец
проекта, войдя тем же аккаунтом Google, на котором заведён проект. Скрипт
печатает обе ссылки уже с именем проекта. [English version](console-steps.md).

**1. Включить вход через Google.**
Откройте `https://console.firebase.google.com/project/<project-id>/authentication/providers`.
Нажмите Get started, если такая кнопка есть, затем Google, Enable. В поле
адреса поддержки выберите свой адрес. Нажмите Save.

**2. Разрешить странице вход со своего адреса.**
Откройте `https://console.cloud.google.com/auth/clients?project=<project-id>`.
Откройте «Web client (auto created by Google Service)». В разделе Authorized
redirect URIs нажмите Add URI и вставьте `https://<site>.web.app/__/auth/handler`.
Нажмите Save.

**Затем** сообщите тому, кто запускает скрипт, что оба шага выполнены.

**Не трогать:** тариф Blaze, оплату и Firebase AI Logic. Проект работает на
бесплатном тарифе, а с включённым AI Logic его квоту сможет расходовать кто
угодно.
