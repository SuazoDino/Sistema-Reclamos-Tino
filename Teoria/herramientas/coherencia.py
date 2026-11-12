import re,glob,sys
def cells(l): return [c.strip().replace('**','').replace('`','') for c in l.strip().strip('|').split('|')]
T={}
for f in sorted(glob.glob('*.md')):
  L=open(f,encoding='utf-8').read().split('\n'); i=0
  while i<len(L):
    if L[i].startswith('| Nombre |') and 'Data Type' not in L[i]:
      info={}
      while i<len(L) and L[i].startswith('|'):
        c=cells(L[i]);
        if not set(''.join(c))<=set('-: '): info[c[0]]=c[1]
        i+=1
      while not L[i].startswith('| Columnas |'): i+=1
      i+=2; cols=[]
      while i<len(L) and L[i].startswith('|'): cols.append(cells(L[i])); i+=1
      T[info['Nombre']]=(f,info,cols)
    else: i+=1
err=[]
def r(ic,rl): n=(ic-4)//(rl+3); return n,(ic-4)-n*(rl+3)
colinfo={}
for n,(f,info,cols) in T.items():
  lr=re.findall(r'\d+',info['Longitud Registro']); mx=int(lr[0]); mo=int(lr[-1])
  total=cols[-1][2]; body=cols[:-1]
  vec=[c for c in body if 'vector' in c[0]]
  if vec:
    fijo=sum(int(c[2]) for c in body if c[2].isdigit() and not c[0].startswith('·'))
    el=sum(int(c[2]) for c in body if c[0].startswith('·'))
    if str(el)+' c/u'!=vec[0][2]: err.append(f'{n}: elemento {el} != {vec[0][2]}')
  else:
    s=sum(int(re.findall(r'\d+',c[2])[0]) for c in body)
    if s!=mx: err.append(f'{n}: suma {s} != MAX {mx}')
  ic=int(info['IC']); org=info['Organización']
  ter=(3072,3584,4096) if org=='secuencial' else (512,1024,1536)
  if ic not in ter: err.append(f'{n}: IC {ic} fuera de terna {org}')
  N=int(re.findall(r'\d+',info['Capacidad'])[0])
  best=min(ter,key=lambda x:r(x,mo)[1])
  if not (ic==best or (ic==512 and N*(mo+3)+4<=512)): err.append(f'{n}: IC {ic} pero menor residuo es {best}')
  ac=info['Capacidad'].split(',')[1].strip(' )')
  esperado=f"19*{12288//ic}*{r(ic,mo)[0]}"
  if ac!=esperado: err.append(f'{n}: AC {ac} esperado {esperado}')
  if 'Clave' in info:
    L,U=map(int,re.findall(r'\d+',info['Clave']))
    pk=[c for c in body if len(c)>3 and c[3]=='X']
    if U!=0 or L!=sum(int(c[2]) for c in pk): err.append(f'{n}: Clave {info["Clave"]} vs PK {[c[0] for c in pk]}')
  elif org=='indexada': err.append(f'{n}: indexada sin Clave')
  off=0
  for c in body:
    nm=c[0].replace('·','').strip().split(' (')[0]
    if 'vector' in c[0]: continue
    colinfo.setdefault(nm,[]).append((n,c[1],c[2],c[3] if len(c)>3 else '',c[4] if len(c)>4 else ''))
  if 'Clave alterna' in info:
    m=re.match(r'(.+) \((\d+), (\d+)\)',info['Clave alterna']); nombres=[x.strip() for x in m.group(1).split('+')]
    o=0; pos={}
    for c in body: pos[c[0]]=o; o+=int(re.findall(r'\d+',c[2])[0]) if c[2][0].isdigit() else 0
    ln=sum(int(c[2]) for c in body if c[0] in nombres)
    if (ln,pos[nombres[0]])!=(int(m.group(2)),int(m.group(3))): err.append(f'{n}: clave alterna {info["Clave alterna"]} calc ({ln},{pos[nombres[0]]})')
# mismo nombre, mismo tipo/len
for nm,v in colinfo.items():
  if len({(t,l) for _,t,l,_,_ in v})>1: err.append(f'col {nm}: '+'; '.join(f'{a}={t}({l})' for a,t,l,_,_ in v))
# FKs
pks={nm:[a for a,_,_,p,_ in v if p=='X'] for nm,v in colinfo.items()}
alias={'CodObjeto':['CodProducto','CodServicio'],'CodBien':['CodClasif'],'CodPadre':['CodClasif'],'DestinoV':['CodCondicion'],'DestinoF':['CodCondicion'],
'CodCondicionInicial':['CodCondicion'],'NroMovReclamo':['NroMov (PRR)','NroMov'],'CodTicket':['CodTicket'],'AplicaA':['CodClasif'],'TiposAdmitidos':['CodTipoProblema'],'CodPaso':['CodPaso']}
for nm,v in colinfo.items():
  for a,t,l,p,fk in v:
    if fk=='X':
      dest=alias.get(nm,[nm]); ok=[d for d in dest if pks.get(d)]
      if not ok: err.append(f'FK {a}.{nm} sin tabla donde sea PK')
      for d in ok:
        for b,t2,l2,_,_ in colinfo[d]:
          if colinfo[d] and b in pks[d] and nm not in('AplicaA','TiposAdmitidos','CodObjeto') and (t,l)!=(t2,l2): err.append(f'FK {a}.{nm} {t}({l}) vs PK {b}.{d} {t2}({l2})')
print(len(T),'tablas'); print('\n'.join(err) if err else 'SIN ERRORES')
