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

local function Trans(data)
  con:write(data..'\n')
  con:flush()
end

local function Recv()
  local s = con:read()
  con:flush()
  return s
end

local function Ping()
  Trans('ping#')
end

function ddns.Connect(ip, port)
  con = net.open(ip, port)
  if con then
    timerid = event.timer(30, Ping, math.huge)
    return true
  else
    print('Connection failed...')
    return false
  end
end

function ddns.Close()
  if con then
    print('Close connection...')
    Trans('exit#')
    event.cancel(timerid)
    con:close()
    return true
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.Register(hwaddress)
  if con then
    Trans("registerFF"..hwaddress)
    print('Register to net...')	
    local s = Recv()
    if s == nil then
      print('Response a nil value...')
      Close()
      return false
    elseif s == "failed" then
      print('Register failed...')
      Close()
      return false
    elseif s == "isused" then
    	print('This MAC Address already registred...')
      Close()
      return false
    else
      print('Register succesful! You IP: '..s)
      return s
    end
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.Send(IP, data)
  if con then
    Trans("sendFF"..IP.."FF"..data)
    return true
  else
    print('Please connect to server!')
    return false
  end
end

function ddns.getMessages()
  if con then
    Trans("getbuffer#")
    data = Recv()
    local messages = {}
    if fdelimiter(data, "DD") then
      nsdata = split(data, "DD")
      for k in pairs(nsdata) do
        sdata = split(k, "FF")
        print(sdata[1].." "..sdata[2])
        if sdata[1] ~= nil and sdata[2] ~= nil then
          messages[sdata[1]] = sdata[2]
        end
      end
    else
      sdata = split(data, "FF")
      print(sdata[1].." "..sdata[2])
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

function ddns.RegisterDomain(domain)
  Trans("registerdomainFF"..domain)
  local data = Recv()
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
end

function ddns.UnRegisterDomain(domain)
  Trans("unregisterdomainFF"..domain)
  local data = Recv()
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
end

function ddns.Reslove(domain)
  Trans("resloveFF"..domain)
  local data = Recv()
  if data == "0" then
    print("Unknown error")
    return false
  elseif data == "1" then
    print("Incorrect domain name")
    return false
  elseif data == "2" then
    print("Domain not already created")
    return false
  else
    print("Resloved domain address: "..data)
    return data
  end
end

return ddns
