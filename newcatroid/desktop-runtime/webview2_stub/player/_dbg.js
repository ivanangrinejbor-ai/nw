const fs = require('fs');
const code = fs.readFileSync('player.js', 'utf8');
const mod = { exports: {} };
new Function('module', 'exports', code)(mod, mod.exports);
const NC = mod.exports;
function fe(type, value, children) {
  const kids = [{ name: 'type', text: type }, { name: 'value', text: String(value) }];
  if (children) {
    for (const side of Object.keys(children)) {
      const v = children[side];
      const arr = Array.isArray(v) ? v : [v];
      kids.push({ name: side, kids: arr.map(n => (n && n.name) ? n : fe('NUMBER', String(n))) });
    }
  }
  return { name: 'formulaElement', kids };
}
const proj = NC.createProject();
const xml = '<?xml version="1.0"?><program><header screenWidth="200" screenHeight="200"/>' +
  '<objectList><object type="Sprite" name="F"><lookList/><soundList/><scriptList/></object></objectList></program>';
const e = NC.loadProjectSync(proj, xml, {});
const sp = e.sprites[0];
try { console.log('SIGN=', e.evalFunction('SIGN', fe('FUNCTION', 'SIGN', { leftChild: fe('NUMBER', '5') }), sp)); }
catch (err) { console.log('SIGN ERR', err.stack); }
try { console.log('LIST_SUM=', e.evalFunction('LIST_SUM', fe('FUNCTION', 'LIST_SUM', { leftChild: fe('STRING', 'L') }), sp)); }
catch (err) { console.log('LIST_SUM ERR', err.stack); }
try { console.log('CONCAT=', e.evalOperator('CONCAT', fe('OPERATOR', 'CONCAT', { leftChild: fe('STRING', 'a'), rightChild: fe('STRING', 'b') }), sp)); }
catch (err) { console.log('CONCAT ERR', err.stack); }
