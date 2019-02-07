local ddns = {}

local event = require("event")
local net = require("internet")
local term = require("term")

local function split(s, delimiter)
    result = {}
    for match in (s..delimiter):gmatch("(.-)"..delimiter) do
        table.insert(result, match)
    end
    return result
end

local function fdelimiter(s, delimiter)
  if string.match(s, delimiter) == delimiter then
    return true
  else
    return false
  end
end

local function trans(data)
  con:write(data..'\n')
  con:flush()
end

local function recv(timeout)
  if timeout ~= nil then
    con:setTimeout(timeout)
  else
    con:setTimeout(5)
  end
  local s = con:read()
  con:flush()
  return s
end

local function ping()
  trans('ping#')
  data = recv(1)
  if data ~= "pong" then
    Close("Server not response")
  end
end

function ddns.connect(ip, port)
  con = net.open(ip, port)
  if con then
    timerid = event.timer(30, ping, math.huge)
    return true
  else
    print('Connection failed...')
    con = nil
    return false
  end
end

function ddns.close(reason)
  if con then
    if reason ~= nil then
      print('Closing connection... Reason: '..reason)
    else 
      print('Closing connection...')
    end
    trans('exit#')
    event.cancel(timerid)
    con:close()
    return true
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.register(hwaddress)
  if con then
    trans("registerFF"..hwaddress)
    print('Getting the ip...')	
    local s = recv()
    if s == nil then
      print('Response a nil value...')
      close("Response a nil value")
      return false
    elseif s == "failed" then
      print('Register failed...')
      close("Register failed")
      return false
    elseif s == "isused" then
    	print('This MAC Address already registred...')
      close("This MAC Address already registred")
      return false
    else
      print('Registration successful! Your IP: '..s)
      return s
    end
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.send(IP, port, data)
  if con then
    trans("sendFF"..IP.."FF"..port.."FF"..data)
    return true
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.getMessages(port)
  if con then
    trans("getbuffer#FF"..port)
    data = recv()
    local messages = {}
    if fdelimiter(data, "DD") then
      nsdata = split(data, "DD")
      for k in pairs(nsdata) do
        sdata = split(k, "FF")
        if sdata[1] ~= nil and sdata[2] ~= nil then
          messages[sdata[1]] = sdata[2]
        end
      end
    else
      sdata = split(data, "FF")
      if sdata[1] ~= nil and sdata[2] ~= nil then
        messages[sdata[1]] = sdata[2]
      end
      table.insert(messages, data)
    end
    return messages
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.registerdomain(domain)
  if con then
    trans("registerdomainFF"..domain)
    local data = recv()
    if data == "OK" then
      print("Domain created")
      return true
    elseif data == "0" then
      print("Unknown error")
      return false
    elseif data == "1" then
      print("Incorrect domain name")
      return false
    elseif data == "2" then
      print("Domain already created")
      return false
    elseif data == "3" then
      print("Error create domain file")
      return false
    else
      print("Other error")
      return false
    end
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.unregisterdomain(domain)
  if con then
    trans("unregisterdomainFF"..domain)
    local data = recv()
    if data == "OK" then
      print("Domain deleted")
      return true
    elseif data == "0" then
      print("Unknown error")
      return false
    elseif data == "1" then
      print("Incorrect domain name")
      return false
    elseif data == "2" then
      print("Domain not already created")
      return false
    elseif data == "4" then
      print("Access error to domain file")
      return false
    else
      print("Other error")
      return false
    end
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.resolve(domain)
  if con then
    trans("resloveFF"..domain)
    local data = recv()
    if data == "0" then
      print("Unknown error")
      return false
    elseif data == "1" then
      print("Incorrect domain name")
      return false
    elseif data == "2" then
      print("Domain not already created")
      return false
    elseif data == "5" then
      print("Server bound to the domain is disabled")
      return false
    else
      print("DNS server returned the address: "..data)
      return data
    end
  else
    print('Please connect to server!')
    return false
  end
end

return ddns
