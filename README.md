# OpenComputersDHCP-DNS
<pre>
Server - Исходники проекта на Java (IDE Eclipse)
DHCP-DNS-Server.jar - Скомпилированый файл проекта
ddns.lua - Библиотека для работы с сервером для OpenComputers
</pre>
<h1> Server </h1>
<pre>
Запуск сервера через консоль командой java -jar DHCP-DNS-Server.jar
При первом запуске сервер создаст файл настроек config.properties и
директорию dns для работы с DNS запросами.
</pre>
<h1> Client (OpenComputers ddns.lua) </h1>
<pre>
Для начала работы с библиотекой переместите ее в папку /lib/ вашей машины
и подключите соответствующий файл в вашей программе ddns = require('ddns')

Список методов:
  Connect(IP, Port) - Подключается к серверу по заданному IP и Port. Возвращает true или false
  
  Register(hwaddress) - Запрашивает IP адрес у сервера, hwaddress это адрес интернет-карты на вашей 
                        машине. Возвращает IP или false
                        
  Send(IP, Message) - Отправляет данные на указаный внутриигровой IP. Возвращает true или false
  
  getMessages() - Получает все сообщения которые были отправлены вам в виде пары IP-Данные
                  (переберайте через for IP, data in pairs(messages) do). Возвращает таблицу или false
                  
  RegisterDomain(domain) - Регестрирует указаный домен если он разрешен сервером на адрес 
                           указаный вами при получении IP. Возвращает true или false
                           
  Reslove(domain) - Запрашивает адрес по домену. Возвращает адрес или false
  
  UnRegisterDomain(domain) - Удаляет домен(ВНИМАНИЕ! Домен может быть удален только тем кто его создал). 
                             Возвращает true или false
  
  Close() - Закрывает соеденение с сервером
</pre>
